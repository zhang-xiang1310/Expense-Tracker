"""
Convert TinyBERT to TFLite for Android deployment.
python3 convert_tinybert.py

Output: output/model.tflite, output/vocab.txt
Copy into: app/src/main/assets/
"""
import os, sys
sys.stdout.reconfigure(encoding="utf-8")

import tensorflow as tf
from transformers import AutoTokenizer, TFAutoModel
import numpy as np

MODEL_NAME = "huawei-noah/TinyBERT_General_4L_312D"
OUTPUT_DIR = os.path.join(os.path.dirname(__file__) or ".", "output")
SEQ_LEN = 128


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # 1. Load PyTorch weights into TF model
    print(f"Loading {MODEL_NAME} (PT->TF)...")
    tf_model = TFAutoModel.from_pretrained(MODEL_NAME, from_pt=True)

    # 2. Save vocab
    print("Saving vocab...")
    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
    tokenizer.save_vocabulary(OUTPUT_DIR)

    # 3. Define concrete function for TFLite (pooler_output only)
    input_spec = (
        tf.TensorSpec([1, SEQ_LEN], tf.int32, name="input_ids"),
        tf.TensorSpec([1, SEQ_LEN], tf.int32, name="attention_mask"),
        tf.TensorSpec([1, SEQ_LEN], tf.int32, name="token_type_ids"),
    )

    class BertPooler(tf.keras.Model):
        def __init__(self, bert):
            super().__init__()
            self.bert = bert

        @tf.function(input_signature=[input_spec])
        def serve(self, inputs):
            out = self.bert(inputs)
            return {"embedding": out.pooler_output}

    pooler = BertPooler(tf_model)
    # Warm-up call to build the graph
    dummy = (
        tf.constant(np.random.randint(0, 30522, (1, SEQ_LEN)), tf.int32),
        tf.constant(np.ones((1, SEQ_LEN)), tf.int32),
        tf.constant(np.zeros((1, SEQ_LEN)), tf.int32),
    )
    _ = pooler.serve(dummy)

    # 4. Save SavedModel
    saved_model_path = os.path.join(OUTPUT_DIR, "saved_model")
    print("Saving TF SavedModel...")
    tf.saved_model.save(
        pooler,
        saved_model_path,
        signatures={"serving_default": pooler.serve},
    )
    print(f"  -> {saved_model_path}")

    # 5. Convert to TFLite
    tflite_path = os.path.join(OUTPUT_DIR, "tinybert.tflite")
    print("TF -> TFLite (builtins only)...")
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_path)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"  -> {tflite_path} ({os.path.getsize(tflite_path)/1024/1024:.1f} MB)")

    # 6. Summary
    print("\n=== Done ===")
    print("Copy to Android assets:")
    print(f"  copy output\\tinybert.tflite  ->  app\\src\\main\\assets\\")
    print(f"  copy output\\vocab.txt       ->  app\\src\\main\\assets\\")
    for f in sorted(os.listdir(OUTPUT_DIR)):
        fpath = os.path.join(OUTPUT_DIR, f)
        if os.path.isfile(fpath):
            print(f"  {f}: {os.path.getsize(fpath)/1024:.0f} KB")


if __name__ == "__main__":
    main()
