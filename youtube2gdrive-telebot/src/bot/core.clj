(ns bot.core
  (:gen-class)
  (:require [clojure.core.async :as a]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :as resp]
            [bot.telegram :as tg]
            [bot.ytdlp :as ytdlp]
            [bot.gdrive :as gdrv]
            [bot.dropbox :as dbx]))

(defonce cfg (edn/read-string (slurp (clojure.java.io/resource "config.edn"))))
(defonce gdrv*   (delay (gdrv/client (:google/creds cfg))))
(defonce dbx*    (delay (dbx/client  (:dropbox/token cfg))))

;; ---------- helpers ---------------------------------------------------------
(defn dispatch-upload!
  [dest stream-info]
  (case dest
    :gdrive  (gdrv/upload-stream  @gdrv* (:google/folder-id cfg) stream-info)
    :dropbox (dbx/upload-stream! @dbx*  (:dropbox/folder   cfg) stream-info)))

(defn parse-command
  "Parse user text -> {:mode :video|:audio :dest :gdrive|:dropbox :url <string>}"
  [txt]
  (let [[cmd maybe-dest url] (str/split txt #"\s+")
        mode (case cmd "/video" :video "/audio" :audio nil)
        dest (case maybe-dest "gdrive" :gdrive "dropbox" :dropbox nil)
        url  (if dest url maybe-dest)
        dest (or dest (:default-storage cfg))]
    (when (and mode url) {:mode mode :dest dest :url url})))

(defn process-job [chat-id {:keys [mode dest url]}]
  (tg/send-message (:telegram/token cfg) chat-id
                   (str "Downloading … (" (name dest) ")"))
  (a/go
    (try
      (let [stream-info (ytdlp/video->stream {:url url :mode mode})
            {:keys [link]} (dispatch-upload! dest stream-info)]
        (tg/send-message (:telegram/token cfg) chat-id (str "Here you go:\n" link)))
      (catch Exception e
        (tg/send-message (:telegram/token cfg) chat-id "Failed, sorry.")
        (.printStackTrace e)))))

(defn handler [{:keys [request-method body-params]}]
  (when (= request-method :post)
    (let [txt (get-in body-params ["message" "text"])
          cid (get-in body-params ["message" "chat" "id"])]
      (when-let [job (and txt (parse-command txt))]
        (process-job cid job)))
    (resp/response "ok")))

(defn -main [& _]
  (run-jetty handler {:port 8080 :join? true}))
