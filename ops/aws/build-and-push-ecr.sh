#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
ECR_REPOSITORY_NAME="${ECR_REPOSITORY_NAME:-daymark}"
IMAGE_TAG="${IMAGE_TAG:-$(git rev-parse --short HEAD)}"

if [[ -z "${AWS_ACCOUNT_ID:-}" ]]; then
    echo "AWS_ACCOUNT_ID is required." >&2
    exit 1
fi

IMAGE_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY_NAME}:${IMAGE_TAG}"
LATEST_IMAGE_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY_NAME}:latest"

aws ecr describe-repositories \
    --region "${AWS_REGION}" \
    --repository-names "${ECR_REPOSITORY_NAME}" >/dev/null 2>&1 \
    || aws ecr create-repository \
        --region "${AWS_REGION}" \
        --repository-name "${ECR_REPOSITORY_NAME}" >/dev/null

aws ecr get-login-password --region "${AWS_REGION}" \
    | docker login \
        --username AWS \
        --password-stdin "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

docker build --platform linux/amd64 -t "${IMAGE_URI}" -t "${LATEST_IMAGE_URI}" .
docker push "${IMAGE_URI}"
docker push "${LATEST_IMAGE_URI}"

echo "Pushed ${IMAGE_URI}"
