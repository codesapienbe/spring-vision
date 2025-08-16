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
    embedding = DeepFace.represent(img_array, model_name='Facenet', enforce_detection=True)[0]['embedding']
    return embedding

def create_consumer():
    """Create and configure Kafka consumer."""
    return KafkaConsumer(
        'face-tasks',
        bootstrap_servers='localhost:9092',
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
