# Stage 1: Build the uberjar
FROM clojure:tools-deps-1.11.1.1413 as builder

WORKDIR /app

# Copy dependency file
COPY deps.edn .

# Download dependencies
RUN clojure -P
RUN clojure -Sdeps '{:deps {com.google.api-client/google-api-client {:mvn/version "2.4.0"}}}'

# Copy the rest of the source code
COPY src ./src
COPY resources ./resources

# Create the uberjar
RUN clojure -T:build uber

# Stage 2: Create the final image
FROM openjdk:11-jre-slim

WORKDIR /app

# Install yt-dlp
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip && \
    pip3 install yt-dlp && \
    apt-get remove -y python3-pip && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

# Copy the uberjar from the builder stage
COPY --from=builder /app/target/youtube2gdrive-telebot-standalone.jar .

# Expose the port the app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "youtube2gdrive-telebot-standalone.jar"]
