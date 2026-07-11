#!/bin/bash

set -e
GREEN="\033[32m"
RED="\033[31m"
YELLOW="\033[33m"
END="\033[0m"
echo -e "${GREEN}"
echo "======================================"
echo "        Docker 一键安装脚本"
echo "======================================"
echo -e "${END}"
#########################################
# Root 判断
#########################################
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 sudo 执行该脚本${END}"
    exit 1
fi
#########################################
# 获取脚本目录
#########################################
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MIRROR_CONFIG="$SCRIPT_DIR/docker-mirrors.conf"
#########################################
# 系统检测
#########################################
echo
echo ">>> 检测系统环境..."
if [ ! -f /etc/os-release ]; then
    echo "无法识别系统"
    exit 1
fi
source /etc/os-release
case "$ID" in
ubuntu)
OS_TYPE="ubuntu"
PKG="apt"
;;
debian)
OS_TYPE="debian"
PKG="apt"
;;
centos)
OS_TYPE="centos"
PKG="yum"
;;
rocky)
OS_TYPE="centos"
PKG="dnf"
;;
*)
echo -e "${RED}"
echo "暂不支持系统:"
echo "$PRETTY_NAME"
echo -e "${END}"
exit 1
;;
esac
echo "系统: $PRETTY_NAME"
echo "类型: $OS_TYPE"
#########################################
# Docker安装检测
#########################################
echo
echo ">>> 检测Docker安装状态..."
if command -v docker >/dev/null 2>&1
then
    echo
    echo -e "${YELLOW}"
    echo "检测到当前系统已经安装Docker"
    echo "$(docker --version)"
    echo -e "${END}"
    echo
    echo "请选择操作:"
    echo
    echo "1) 保留当前Docker，退出安装"
    echo "2) 卸载Docker并重新安装"
    echo "3) 跳过检测，继续安装"
    echo
    while true
    do
    read -p "请输入选择 [1/2/3]: " dockerChoice
    case $dockerChoice in
    1)
        echo "退出安装"
        exit 0
        ;;
    2)
        echo
        echo ">>> 卸载Docker..."
        systemctl stop docker || true
        if [ "$PKG" = "apt" ]; then
            apt-get remove -y \
            docker-ce \
            docker-ce-cli \
            containerd.io \
            docker-buildx-plugin \
            docker-compose-plugin \
            || true
        else
            $PKG remove -y \
            docker-ce \
            docker-ce-cli \
            containerd.io \
            docker-buildx-plugin \
            docker-compose-plugin \
            || true
        fi
        echo
        read -p "是否删除Docker数据目录(/var/lib/docker)? (y/n): " deleteData
        if [[ "$deleteData" =~ ^[Yy]$ ]]
        then
            rm -rf /var/lib/docker
            rm -rf /var/lib/containerd
            echo "Docker数据已删除"
        fi
        break
        ;;
    3)
        echo "继续安装"
        break
        ;;
    *)
        echo "输入错误"
        ;;
    esac
    done
fi
#########################################
# 安装基础依赖
#########################################
echo
echo ">>> 安装基础依赖..."
if [ "$PKG" = "apt" ]; then
    apt-get update
    apt-get install -y \
    curl \
    ca-certificates \
    gnupg \
    lsb-release
else
    $PKG install -y \
    curl \
    ca-certificates \
    gnupg2
fi
#########################################
# 网络区域检测
#########################################
echo
echo ">>> 检测网络出口区域..."
COUNTRY=$(curl -s \
--connect-timeout 3 \
https://ipapi.co/country_name || echo "未知")
echo
echo "检测结果:"
echo "当前公网出口区域: $COUNTRY"
#########################################
# 选择安装路线
#########################################
echo
echo "请选择Docker安装方案:"
echo
echo "1) 国内网络方案"
echo "   - 使用国内Docker软件源"
echo "   - 支持配置镜像加速"
echo
echo "2) 国际网络方案"
echo "   - 使用Docker官方软件源"
echo "   - 不配置镜像加速"
echo
while true
do
read -p "请输入选择 [1/2]: " choice
case $choice in
1)
REGION="CN"
break
;;
2)
REGION="GLOBAL"
break
;;
*)
echo "输入错误，请重新选择"
;;
esac
done
echo
if [ "$REGION" = "CN" ]; then
    echo -e "${GREEN}已选择: 国内网络方案${END}"
else
    echo -e "${GREEN}已选择: 国际网络方案${END}"
fi
#########################################
# 配置Docker软件源
#########################################
echo
echo ">>> 配置Docker软件源..."
mkdir -p /etc/apt/keyrings
if [ "$OS_TYPE" = "ubuntu" ] || [ "$OS_TYPE" = "debian" ]; then
    #################################
    # Debian / Ubuntu
    #################################
    if [ "$REGION" = "CN" ]; then
        echo ">>> 使用国内Docker源"
        if [ "$OS_TYPE" = "ubuntu" ]; then
            DOCKER_REPO="https://mirrors.aliyun.com/docker-ce/linux/ubuntu"
        else
            DOCKER_REPO="https://mirrors.aliyun.com/docker-ce/linux/debian"
        fi
    else
        echo ">>> 使用Docker官方源"
        if [ "$OS_TYPE" = "ubuntu" ]; then
            DOCKER_REPO="https://download.docker.com/linux/ubuntu"
        else
            DOCKER_REPO="https://download.docker.com/linux/debian"
        fi
    fi
    if [ -f /etc/apt/keyrings/docker.gpg ]; then
        echo
        echo "检测到 Docker GPG 密钥已存在: /etc/apt/keyrings/docker.gpg"
        read -p "是否覆盖? [Y/n, 默认 Y]: " overwriteGpg
        overwriteGpg=${overwriteGpg:-Y}
        if [[ "$overwriteGpg" =~ ^[Yy]$ ]]; then
            rm -f /etc/apt/keyrings/docker.gpg
        fi
    fi

    if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
        if [ "$REGION" = "CN" ]; then
            curl -fsSL \
            https://mirrors.aliyun.com/docker-ce/linux/$OS_TYPE/gpg \
            | gpg --dearmor \
            -o /etc/apt/keyrings/docker.gpg
        else
            curl -fsSL \
            https://download.docker.com/linux/$OS_TYPE/gpg \
            | gpg --dearmor \
            -o /etc/apt/keyrings/docker.gpg
        fi
    else
        echo "跳过 GPG 密钥导入，使用已有文件"
    fi
    echo \
"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
$DOCKER_REPO \
$(lsb_release -cs) stable" \
> /etc/apt/sources.list.d/docker.list
else
    #################################
    # CentOS / Rocky
    #################################
    echo ">>> 配置CentOS系列Docker源"
    if [ "$REGION" = "CN" ]; then
        REPO="https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo"
    else
        REPO="https://download.docker.com/linux/centos/docker-ce.repo"
    fi
    yum install -y yum-utils || true
    yum-config-manager \
    --add-repo \
    "$REPO"
fi
#########################################
# 安装Docker
#########################################
echo
echo ">>> 安装Docker..."
if [ "$PKG" = "apt" ]; then
    apt-get update
    apt-get install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin
else
    $PKG install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin
fi
#########################################
# 启动Docker
#########################################
echo
echo ">>> 启动Docker..."
systemctl daemon-reload
systemctl enable docker
systemctl start docker
#########################################
# 国内镜像加速
#########################################
if [ "$REGION" = "CN" ]; then
echo
read -p "是否配置Docker镜像加速(y/n): " configMirror
if [[ "$configMirror" =~ ^[Yy]$ ]]; then
    if [ -f "$MIRROR_CONFIG" ]; then
        echo
        echo ">>> 配置Docker镜像加速"
        mkdir -p /etc/docker
        mirrors=$(grep -v "^#" "$MIRROR_CONFIG" | grep -v "^$" | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sed 's/"//g')
        if [ -n "$mirrors" ]; then
            mirror_json=$(echo "$mirrors" | awk '{printf "    \"%s\",\n", $0}' | sed '$s/,$//')
            cat >/etc/docker/daemon.json <<EOF
{
  "registry-mirrors": [
$mirror_json
  ]
}
EOF
            echo
            echo "daemon.json:"
            cat /etc/docker/daemon.json
            systemctl daemon-reload
            if ! systemctl restart docker; then
                echo -e "${RED}ERROR: Docker 重启失败！/etc/docker/daemon.json 格式有误${END}"
                exit 1
            fi
        else
            echo "镜像配置为空，跳过"
        fi
    else
        echo "未找到docker-mirrors.conf，跳过"
    fi
else
    echo "跳过镜像加速配置"
fi
else
echo
echo "国际网络方案，不配置镜像加速"
fi
#########################################
# Docker测试
#########################################
echo
echo ">>> 测试Docker运行..."
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Docker 守护进程未正常运行，跳过测试${END}"
    exit 1
fi
docker run hello-world
#########################################
# 清理测试资源
#########################################
echo
echo ">>> 清理测试资源..."
docker rm \
$(docker ps -aq --filter ancestor=hello-world) \
2>/dev/null || true
docker rmi hello-world \
2>/dev/null || true
#########################################
# 输出版本
#########################################
echo
echo "======================================"
docker --version
echo
docker compose version || true
echo
systemctl status docker \
--no-pager
echo
echo -e "${GREEN}"
echo "Docker安装完成!"
echo -e "${END}"

# 查看 Docker 服务状态
systemctl status docker

# 启动并设为开机自启
systemctl start docker
systemctl enable docker

# 确认状态
systemctl status docker --no-pager
docker ps -a