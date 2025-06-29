#!/bin/bash

# 设置变量
KEYSTORE=keystore.p12
KEYSTORE_PASS=123456
ALIAS=tomcat
VALIDITY=3650 # 10年有效期

# 生成自签名证书
keytool -genkeypair \
  -alias $ALIAS \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore $KEYSTORE \
  -validity $VALIDITY \
  -storepass $KEYSTORE_PASS \
  -dname "CN=localhost, OU=Development, O=shiwujieGatewaty, L=City, ST=State, C=CN" \
  -ext "SAN=DNS:localhost,IP:192.168.5.142"

echo "证书已生成: $KEYSTORE"
echo "密码: $KEYSTORE_PASS"
echo "别名: $ALIAS" 