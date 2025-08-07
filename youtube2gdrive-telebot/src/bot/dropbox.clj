(ns bot.dropbox
  (:import [com.dropbox.core DbxRequestConfig]
           [com.dropbox.core.v2 DbxClientV2]
           [com.dropbox.core.v2.files WriteMode]
           [java.util Locale]))

(defn client [token]
  (DbxClientV2. (DbxRequestConfig. "yt-bot" (Locale/getDefault)) token))

(def chunk-size (* 8 1024 1024))                     ;; 8 MiB

(defn upload-stream!
  [^DbxClientV2 cli folder {:keys [stream filename]}]
  (let [path (str (when (not= folder "/") folder) "/" filename)]
    (with-open [in stream]
      (if (< (.available in) (* 150 1024 1024))
        (do
          (.. cli files (uploadBuilder path)
              (withMode WriteMode/OVERWRITE)
              uploadAndFinish in)
          (let [link (.. cli sharing (createSharedLinkWithSettings path))]
            {:link (.getUrl link)}))
        ;; large file → session
        (let [first-chunk (.readNBytes in chunk-size)]
          (when (pos? (alength first-chunk))
            (let [start-result (.. cli files
                                   (uploadSessionStart)
                                   (uploadAndFinish (java.io.ByteArrayInputStream. first-chunk)))
                  session-id (.getSessionId start-result)]
              (loop [offset (long (alength first-chunk))]
                (let [buf (.readNBytes in chunk-size)]
                  (if (pos? (alength buf))
                    (do
                      (let [cursor (com.dropbox.core.v2.files.UploadSessionCursor. session-id offset)]
                        (.. cli files (uploadSessionAppendV2 cursor)
                            (uploadAndFinish (java.io.ByteArrayInputStream. buf))))
                      (recur (+ offset (alength buf))))
                    (let [cursor (com.dropbox.core.v2.files.UploadSessionCursor. session-id offset)
                          commit (-> (com.dropbox.core.v2.files.CommitInfo/newBuilder path)
                                     (.withMode WriteMode/OVERWRITE)
                                     .build)]
                      (.. cli files (uploadSessionFinish cursor commit))))))))
          (let [link (.. cli sharing (createSharedLinkWithSettings path))]
            {:link (.getUrl link)}))))))
