import os
# Force TensorFlow to use CPU only
os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'  # Reduce TensorFlow logging

from kafka import KafkaConsumer
from deepface import DeepFace
import numpy as np
from io import BytesIO
from PIL import Image
import sys
import signal
from loguru import logger

def process_image_bytes(image_bytes):
    """Process image bytes and return face embedding."""
    # Load image (no disk) — if image is already JPEG/PNG bytes
    image = Image.open(BytesIO(image_bytes)).convert('RGB')
    img_array = np.array(image)
    result = DeepFace.represent(img_array, model_name='Facenet', enforce_detection=True)
    embedding = result[0]['embedding']
    return embedding

def create_consumer():
    """Create and configure Kafka consumer."""
    # Get configuration from environment variables
    bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
    topic = os.getenv('KAFKA_TOPIC', 'face-tasks')
    log_level = os.getenv('LOG_LEVEL', 'INFO')

    # Configure logging
    logger.remove()
    logger.add(sys.stderr, level=log_level)

    logger.info(f"Connecting to Kafka at {bootstrap_servers}")
    logger.info(f"Listening to topic: {topic}")

    return KafkaConsumer(
        topic,
        bootstrap_servers=bootstrap_servers,
        auto_offset_reset='earliest',
        enable_auto_commit=True,
        group_id='face-worker-group',
        key_deserializer=lambda k: k.decode() if k else None,
        value_deserializer=lambda x: x  # raw bytes as message value
    )

def main():
    """Main function to run the DeepFace consumer."""
    logger.info("Starting DeepFace consumer...")

    # Setup signal handlers for graceful shutdown
    def signal_handler(signum, frame):
        logger.info("Received shutdown signal, stopping consumer...")
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    consumer = create_consumer()
    logger.info("Consumer created, waiting for messages...")

    try:
        for message in consumer:
            user_key = message.key  # "userId:filename"
            image_bytes = message.value

            logger.info(f"Processing message for user: {user_key}")

            try:
                embedding = process_image_bytes(image_bytes)
                logger.info(f"User {user_key} embedding (len={len(embedding)}): {embedding[:5]} ...")
                # Save embedding to vector DB here (e.g., Pinecone/Milvus/Redis), all RAM
            except Exception as e:
                logger.error(f"Failed to process {user_key}: {e}")

    except KeyboardInterrupt:
        logger.info("Consumer interrupted by user")
    except Exception as e:
        logger.error(f"Consumer error: {e}")
    finally:
        consumer.close()
        logger.info("Consumer stopped")

if __name__ == "__main__":
    main()
