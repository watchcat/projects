(ns bot.gdrive
  (:require [clojure.java.io :as io])
  (:import [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.services.drive Drive$Builder]
           [com.google.auth.oauth2 ServiceAccountCredentials]
           [com.google.api.client.http InputStreamContent]
           [com.google.api.services.drive.model File]))

(defn client [creds-file]
  (let [cred (-> (ServiceAccountCredentials/fromStream (io/input-stream creds-file))
                 (.createScoped ["https://www.googleapis.com/auth/drive"]))]
    (-> (Drive$Builder.
          (GoogleNetHttpTransport/newTrustedTransport)
          (JacksonFactory/getDefaultInstance)
          cred)
        (.setApplicationName "youtube2gdrive-bot")
        .build)))

(defn upload-stream
  [^com.google.api.services.drive.Drive drive folder-id {:keys [stream filename]}]
  (let [meta  (doto (File.)
                (.setName filename)
                (.setParents (java.util.Collections/singletonList folder-id)))
        media (InputStreamContent.
               (if (.endsWith filename ".mp3") "audio/mpeg" "video/mp4")
               stream)
        file  (.. drive files (create meta media) (setFields "id,webViewLink") execute)]
    {:link (.getWebViewLink file)}))

(defn list-files
  [^com.google.api.services.drive.Drive drive folder-id]
  (let [query (str "'" folder-id "' in parents")
        fields "files(id,name)"
        result (.. drive files list (setQ query) (setFields fields) execute)]
    (map (fn [^File f] {:id (.getId f) :name (.getName f)})
         (.getFiles result))))

(defn delete-file
  [^com.google.api.services.drive.Drive drive file-id]
  (.. drive files (delete file-id) execute))
