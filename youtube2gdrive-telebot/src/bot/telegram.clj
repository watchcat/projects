(ns bot.telegram
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def api "https://api.telegram.org")

(defn request
  [token method body]
  (http/post (str api "/bot" token "/" method)
             {:as           :json
              :content-type :json
              :body         (json/encode body)}))

(defn send-message
  [token chat-id text & {:keys [reply_markup]}]
  (request token "sendMessage"
           (cond-> {:chat_id    chat-id
                    :text       text
                    :disable_web_page_preview true}
             reply_markup (assoc :reply_markup reply_markup))))
