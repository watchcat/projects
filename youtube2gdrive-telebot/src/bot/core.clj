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
  "Parse user text -> {:command :video|:audio|:drivelist|:drivedelete ...}"
  [txt]
  (let [[cmd & args] (str/split txt #"\s+")]
    (case cmd
      "/video"
      (let [[maybe-dest url] args
            dest (case maybe-dest "gdrive" :gdrive "dropbox" :dropbox nil)
            url  (if dest url maybe-dest)
            dest (or dest (:default-storage cfg))]
        (when url {:command :video :dest dest :url url}))

      "/audio"
      (let [[maybe-dest url] args
            dest (case maybe-dest "gdrive" :gdrive "dropbox" :dropbox nil)
            url  (if dest url maybe-dest)
            dest (or dest (:default-storage cfg))]
        (when url {:command :audio :dest dest :url url}))

      "/drivelist"
      {:command :drivelist}

      "/drivedelete"
      (when-let [file-id (first args)]
        {:command :drivedelete :file-id file-id})

      nil)))

(defn process-command [chat-id {:keys [command] :as job}]
  (case command
    (:video :audio)
    (let [{:keys [dest url]} job
          mode (if (= command :video) :video :audio)]
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

    :drivelist
    (a/go
      (try
        (let [files (gdrv/list-files @gdrv* (:google/folder-id cfg))]
          (if (seq files)
            (let [file-list (str/join "\n" (for [{:keys [id name]} files]
                                            (str name " - " id)))]
              (tg/send-message (:telegram/token cfg) chat-id (str "Files in Drive:\n" file-list)))
            (tg/send-message (:telegram/token cfg) chat-id "No files found in Drive folder.")))
        (catch Exception e
          (tg/send-message (:telegram/token cfg) chat-id "Failed to list files.")
          (.printStackTrace e))))

    :drivedelete
    (let [{:keys [file-id]} job]
      (a/go
        (try
          (gdrv/delete-file @gdrv* file-id)
          (tg/send-message (:telegram/token cfg) chat-id (str "File " file-id " deleted."))
          (catch Exception e
            (tg/send-message (:telegram/token cfg) chat-id "Failed to delete file.")
            (.printStackTrace e)))))))

(defn handler [{:keys [request-method body]}]
  (when (= request-method :post)
    (let [txt (get-in body ["message" "text"])
          cid (get-in body ["message" "chat" "id"])]
      (when-let [job (and txt (parse-command txt))]
        (process-command cid job))))
  (resp/response "ok"))

(defn -main [& _]
  (run-jetty handler {:port 8080 :join? true}))
