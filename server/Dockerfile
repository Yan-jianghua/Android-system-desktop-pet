FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 QIUQIU_DATA_DIR=/data QIUQIU_PORT=8080
WORKDIR /app
COPY app.py /app/app.py
RUN useradd --system --uid 10001 qiuqiu && mkdir -p /data && chown -R qiuqiu:qiuqiu /data /app
USER qiuqiu
EXPOSE 8080
VOLUME ["/data"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8080/v1/health',timeout=3)"
CMD ["python", "/app/app.py"]
