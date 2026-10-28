FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && useradd --uid 10001 --create-home inbox && mkdir /data && chown inbox:inbox /data
COPY app.py family_clipboard.py clipboard-manage.html ./
USER inbox
EXPOSE 8765
CMD ["uvicorn","app:factory","--factory","--host","0.0.0.0","--port","8765","--proxy-headers","--forwarded-allow-ips=*","--no-access-log"]
