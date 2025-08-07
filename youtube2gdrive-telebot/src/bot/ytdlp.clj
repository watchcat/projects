(ns bot.ytdlp
  (:require [clojure.java.io :as io]))

(def yt-bin "yt-dlp")                                ;; ensure in $PATH

(defn video->stream
  "Return {:stream <InputStream> :filename <string> :process <Process>}"
  [{:keys [url mode]}]
  (let [fmt (if (= mode :audio) "bestaudio" "best[ext=mp4]")
        args (concat [yt-bin "-f" fmt "-o" "-" url]
                     (when (= mode :audio)
                       ["--extract-audio" "--audio-format" "mp3" "--audio-quality" "0"]))
        pb   (ProcessBuilder. (into-array String args))
        _    (.redirectError pb ProcessBuilder$Redirect/INHERIT)
        p    (.start pb)]
    {:stream   (.getInputStream p)
     :filename (str (System/currentTimeMillis)
                    (if (= mode :audio) ".mp3" ".mp4"))
     :process  p}))
