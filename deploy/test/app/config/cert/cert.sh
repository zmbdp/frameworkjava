#!/bin/bash

#vi /lib/systemd/system/docker.service
#--tlsverify --tlscacert=/etc/docker/ca.pem --tlscert=/etc/docker/server-cert.pem --tlskey=/etc/docker/server-key.pem

SERVER="10.0.16.13"
PASSWORD="123456"
COUNTRY="CN"
STATE="ShanXi"
CITY="XiAn"
ORGANIZATION="zmbdp"
ORGANIZATIONAL_UNIT="Dev"
EMAIL="123@139.com"

#临时文件夹
TEMP_DIR='/data/cert/docker'

mkdir -p ${TEMP_DIR}
cd ${TEMP_DIR}

#生成`ca-key.pem` 文件 设置密码
openssl genrsa -aes256 -passout pass:${PASSWORD} -out ca-key.pem 4096
echo "生成ca私钥完成"


#生成 `ca.pem` 文件，设置国家、省市、组织名、邮箱

openssl req -new -x509  -passin "pass:${PASSWORD}"  -days 3650 -key ca-key.pem -sha256 -out ca.pem -subj "/C=${COUNTRY}/ST=${STATE}/L=${CITY}/O=${ORGANIZATION}/OU=${ORGANIZATIONAL_UNIT}/emailAddress=${EMAIL}"
echo "填写配置信息完成"

#生成 `server-key.pem` 文件

openssl genrsa -out server-key.pem 4096

#生成 `server.csr` 文件

openssl req -subj "/CN=${SERVER}" -sha256 -new -key server-key.pem -out server.csr

#生成配置文件 `extfile.cnf`

echo subjectAltName = IP:${SERVER},IP:0.0.0.0 >> extfile.cnf

echo extendedKeyUsage = serverAuth >> extfile.cnf

#生成服务器证书，需要输入之前输入的密码
openssl x509 -req -days 3650 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem  -passin "pass:${PASSWORD}"  \-CAcreateserial -out server-cert.pem -extfile extfile.cnf

echo "生成自签证书完成"

#添加配置，使密钥适合客户端身份验证
echo extendedKeyUsage = clientAuth >> extfile.cnf
#生成 `key.pem` 文件
openssl genrsa -out key.pem 4096
#创建`client.csr`文件
openssl req -subj '/CN=client' -new -key key.pem -out client.csr
#生成证书
openssl x509 -req -days 3650 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -passin "pass:${PASSWORD}" \-CAcreateserial -out cert.pem -extfile extfile.cnf
echo "生成client自签证书完成"
rm -v -f client.csr server.csr
chmod -v 0444 ca-key.pem key.pem server-key.pem
chmod -v 0444 ca.pem server-cert.pem cert.pem
echo "复制证书到指定目录"
cp server-*.pem  /etc/docker/
cp ca.pem /etc/docker/

