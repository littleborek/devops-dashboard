# DevOps Dashboard Uzak Sunucu Ajanı (Agent)

Bu script, uzak sunucularınızdaki (Oracle Cloud, AWS, DigitalOcean vb.) metrikleri (CPU, RAM) ve Docker konteyner bilgilerini merkeze göndermek için tasarlanmıştır.

## 🚀 Kurulum

1. Uzak sunucunuza `agent_monitor.py` dosyasını kopyalayın.
2. Dosya içerisindeki `MASTER_URL` değişkenini ana sunucunuzun IP adresiyle güncelleyin:
   ```python
   MASTER_URL = "http://95.xx.xx.xx:8080/api/v1/agent/sync"
   ```
3. Script'in çalışması için Python 3 gereklidir. Ekstra bir kütüphane (library) yüklemenize gerek yoktur, standart kütüphanelerle çalışır.

## ⏲️ Cron Job Ayarı (Otomatik Çalıştırma)

Metriklerin her dakika otomatik gönderilmesi için sunucunuzda bir cron job oluşturun:

1. Terminalde `crontab -e` komutunu çalıştırın.
2. Açılan dosyanın en altına şu satırı ekleyin (dosya yolunu kendinize göre güncelleyin):
   ```bash
   * * * * * /usr/bin/python3 /home/ubuntu/agent_monitor.py >> /home/ubuntu/agent.log 2>&1
   ```

## 🛡️ Önemli Not
Güvenlik nedeniyle, bir sunucunun veri gönderebilmesi için **önce Dashboard paneli üzerinden "Yeni Sunucu" diyerek o sunucunun IP adresiyle bir kayıt oluşturmanız gerekmektedir.** Tanınmayan IP adreslerinden gelen veriler reddedilir.
