# YouTube to Google Drive/Dropbox Telegram Bot

This is a Telegram bot that can download YouTube videos as video or audio and upload them to Google Drive or Dropbox.

## Configuration

1.  Rename `resources/config.edn.example` to `resources/config.edn`.
2.  Open `resources/config.edn` and fill in the required values:

    -   `:telegram/token`: Your Telegram bot token from BotFather.
    -   `:google/creds`: Path to your Google service account credentials JSON file. See [Google Cloud documentation](https://cloud.google.com/iam/docs/creating-managing-service-account-keys) for more details.
    -   `:google/folder-id`: The ID of the Google Drive folder where files should be uploaded.
    -   `:dropbox/token`: Your Dropbox app token.
    -   `:dropbox/folder`: The path to the Dropbox folder for uploads (e.g., `/youtube-downloads`).
    -   `:default-storage`: The default upload destination, either `:gdrive` or `:dropbox`.
    -   `:base-url`: The base URL where your bot's webhook is publicly accessible (e.g., `https://your-domain.com`).

## Installation

This project uses `clojure` and `deps.edn` for dependency management. You will need to have the [Clojure CLI tools](https://clojure.org/guides/getting_started) installed.

The project also requires `yt-dlp` to be installed and available in your system's `$PATH`. See the [yt-dlp documentation](https://github.com/yt-dlp/yt-dlp#installation) for installation instructions.

Once you have the prerequisites, the dependencies will be downloaded automatically when you run the application.

## Running the Bot

To run the bot, execute the following command from the project root:

```bash
clj -M:run
```

The bot will start a web server on port 8080 to listen for webhook requests from Telegram.

## Setting up the Telegram Webhook

For the bot to receive messages from Telegram, you need to set up a webhook. You can do this by sending a request to the Telegram Bot API.

Replace `YOUR_BOT_TOKEN` and `YOUR_WEBHOOK_URL` with your actual bot token and the URL where your bot is running. The webhook URL should be the `:base-url` from your config file, with the `/webhook` path appended if your handler is mapped to that. Since this bot's handler is at the root, the URL is just the `:base-url`.

```
https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook?url=<YOUR_WEBHOOK_URL>
```

You can open this URL in your browser or use a tool like `curl` to set the webhook.

Once the webhook is set, your bot will receive updates from Telegram at the specified URL.

## Usage

Once the bot is running and the webhook is set, you can send commands to it in your Telegram chat:

-   `/video <url>`: Downloads the video from the given URL and uploads it to the default storage.
-   `/audio <url>`: Extracts the audio from the video at the given URL (as mp3) and uploads it to the default storage.
-   `/video <gdrive|dropbox> <url>`: Downloads the video and uploads it to the specified storage.
-   `/audio <gdrive|dropbox> <url>`: Extracts the audio and uploads it to the specified storage.
-   `/drivelist`: Lists all files in the configured Google Drive folder.
-   `/drivedelete <file-id>`: Deletes a file from the Google Drive folder using its ID.

## Deployment to Google Cloud

This project includes a script to deploy the bot to [Google Cloud Run](https://cloud.google.com/run).

### Prerequisites

1.  [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) installed and initialized.
2.  You must be authenticated with gcloud: `gcloud auth login`.
3.  You must have a Google Cloud project created.

### Steps

1.  **Edit `deploy.sh`**: Open the `deploy.sh` script and set your `GCP_PROJECT_ID` and optionally the `GCP_REGION`.
2.  **Run the script**:
    ```bash
    ./deploy.sh
    ```
3.  The script will:
    -   Enable the required Google Cloud services.
    -   Build the Docker container using Google Cloud Build.
    -   Deploy the container to Google Cloud Run.
    -   Print the URL of the deployed service.
4.  **Set the Webhook**: Use the URL provided at the end of the deployment to set your Telegram webhook as described in the "Setting up the Telegram Webhook" section.
