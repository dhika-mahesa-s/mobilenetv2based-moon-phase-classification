# Dataset

The model was trained on a combined dataset sourced from four different Roboflow Universe project repositories (6 versions in total). After applying MD5 hash-based deduplication to remove duplicate images across versions, 2,905 unique images were retained for training.
## Dataset Sources

- [Repository 1 – v6, v7, v8](https://universe.roboflow.com/smcm-ai-class/phase-your-moon/dataset/8)
- [Repository 2 – v9](https://universe.roboflow.com/horz/moon-fq83l/dataset/1)
- [Repository 3 – v10](https://universe.roboflow.com/neo-8a90a/moon2-oaefy)
- [Repository 2 – v11](https://universe.roboflow.com/fabrizios-workspace-oni1i/moon-project-pbjjp/) 

## How to Download

1. Create a free account at [Roboflow](https://roboflow.com) if you don't have one already.
2. Open each dataset repository link listed above.
3. Select the version you want and click **Download Dataset**.
4. Choose **Multi-Label Classification** or **TensorFlow Keras** as the export format.
5. Download and extract each version into a separate folder.
6. Run the deduplication cell in the notebook to merge all versions and remove duplicate images using MD5 hashing before proceeding to training.