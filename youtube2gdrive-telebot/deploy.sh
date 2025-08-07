#!/usr/bin/env bash
set -euo pipefail

# --------------------------------------------------------------------
#  Configuration
# --------------------------------------------------------------------
# GCP_PROJECT_ID: Your Google Cloud project ID.
# GCP_REGION: The region where you want to deploy the service (e.g., "us-central1").
# SERVICE_NAME: The name of the Cloud Run service.
# --------------------------------------------------------------------
GCP_PROJECT_ID="youtube2gdrive-telebot"
GCP_REGION="us-central1"
SERVICE_NAME="youtube2gdrive-telebot"

# --------------------------------------------------------------------
#  Script Logic
# --------------------------------------------------------------------
if [[ "$GCP_PROJECT_ID" == "your-gcp-project-id" ]]; then
  echo "Please edit deploy.sh and set your GCP_PROJECT_ID."
  exit 1
fi

echo "--- Enabling required Google Cloud services ---"
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  --project="$GCP_PROJECT_ID"

echo "--- Building the container image with Cloud Build ---"
gcloud builds submit --tag "gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME" --project="$GCP_PROJECT_ID"

echo "--- Deploying to Cloud Run ---"
gcloud run deploy "$SERVICE_NAME" \
  --image="gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME" \
  --platform=managed \
  --region="$GCP_REGION" \
  --allow-unauthenticated \
  --project="$GCP_PROJECT_ID"

SERVICE_URL=$(gcloud run services describe "$SERVICE_NAME" --platform=managed --region="$GCP_REGION" --format="value(status.url)" --project="$GCP_PROJECT_ID")

echo "--------------------------------------------------------------------"
echo "✅ Deployment successful!"
echo ""
echo "Your service is available at: $SERVICE_URL"
echo ""
echo "Next step: Set your Telegram webhook to this URL."
echo "You can do this by visiting the following URL in your browser:"
echo "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook?url=$SERVICE_URL"
echo "--------------------------------------------------------------------"
