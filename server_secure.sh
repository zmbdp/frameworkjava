#!/bin/bash
# ============================================================
# 服务器一键安全加固脚本
# 适用系统: Ubuntu 20.04 / 22.04 / 24.04
# 作者: zmbdp
# 目标: 改SSH端口、建用户、防火墙、Fail2ban、自动更新
# ============================================================

set -e

wait_apt() {
    while fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 \
       || fuser /var/lib/apt/lists/lock >/dev/null 2>&1; do
        echo "等待APT锁释放..."
        sleep 3
    done
}

# ------------------- 颜色定义 -------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ------------------- 检查root权限 -------------------
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}[错误] 请使用 sudo 或 root 用户运行此脚本${NC}"
    exit 1
fi
# ------------------- 自动获取信息 -------------------
if [ -f /etc/redhat-release ] || grep -qE "CentOS|Fedora|RHEL" /etc/os-release 2>/dev/null; then
    echo -e "${RED}[错误] 当前系统为RHEL/CentOS/Fedora，请使用适用于该系统的脚本${NC}"
    exit 1
fi

if ! command -v apt-get &>/dev/null; then
    echo -e "${RED}[错误] 当前系统不支持APT包管理器，请使用适用于该系统的脚本${NC}"
    exit 1
fi

LOG_FILE="/var/log/server-secure/server-secure-$(date +%Y%m%d%H%M%S).log"
mkdir -p /var/log/server-secure
exec > >(tee -i "$LOG_FILE") 2>&1

echo -e "${BLUE}正在检测网络环境...${NC}"
PUBLIC_IP=$(curl -s --connect-timeout 5 https://ip.sb 2>/dev/null || \
            curl -s --connect-timeout 5 https://ifconfig.me 2>/dev/null || \
            curl -s --connect-timeout 5 https://api.ipify.org 2>/dev/null || \
            echo "获取失败")

CURRENT_SSH_PORT=$(sshd -T 2>/dev/null | awk '/^port /{print $2;exit}')
[ -z "$CURRENT_SSH_PORT" ] && CURRENT_SSH_PORT="22"

# ------------------- 欢迎界面 -------------------
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}          ${GREEN}服务器一键安全加固脚本${NC}                    ${CYAN}║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  当前公网IP:   ${GREEN}${PUBLIC_IP}${NC}"
echo -e "  当前SSH端口:  ${GREEN}${CURRENT_SSH_PORT}${NC}"
echo -e "  操作系统:     ${GREEN}$(lsb_release -ds 2>/dev/null || cat /etc/os-release | grep PRETTY_NAME | cut -d'"' -f2)${NC}"
echo ""

echo -e "${RED}⚠️  警告：此脚本将修改系统配置，请确保已备份重要数据${NC}"
echo -e "${RED}⚠️  建议：在执行前确认您有VNC/控制台访问权限作为兜底${NC}"
echo ""
read -p "是否继续执行安全加固？[y/N]: " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}已取消执行${NC}"
    exit 0
fi

# ------------------- 用户选择配置 -------------------
echo ""
echo -e "${BLUE}══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}[用户配置] 请根据需要选择以下选项${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════════${NC}"
echo ""

# 1. 是否创建新用户
CREATE_USER="n"
read -p "是否创建新管理员用户？[y/N]: " CREATE_USER

if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    read -p "请输入新用户名: " NEW_USER
    while [ -z "$NEW_USER" ]; do
        echo -e "${RED}! 用户名为空，请重新输入${NC}"
        read -p "请输入新用户名: " NEW_USER
    done

    echo ""
    echo -e "${YELLOW}新用户密码（用于sudo授权）${NC}"
    echo -e "${YELLOW}注：即使禁用SSH密码登录，此密码仍用于sudo提权${NC}"
    read -s -p "请输入新用户密码: " NEW_PASS
    echo ""
    read -s -p "请确认密码: " NEW_PASS_CONFIRM
    echo ""

    while [ "$NEW_PASS" != "$NEW_PASS_CONFIRM" ]; do
        echo -e "${RED}! 两次输入的密码不一致，请重新输入${NC}"
        read -s -p "请输入新用户密码: " NEW_PASS
        echo ""
        read -s -p "请确认密码: " NEW_PASS_CONFIRM
        echo ""
    done
fi

# 2. 是否配置root公钥
CONFIG_ROOT_KEY="n"
read -p "是否配置root用户SSH公钥？[y/N]: " CONFIG_ROOT_KEY

if [[ "$CONFIG_ROOT_KEY" =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${YELLOW}请粘贴root用户的SSH公钥（粘贴完成后按回车结束）${NC}"
    read -r ROOT_PUBLIC_KEY
    ROOT_PUBLIC_KEY=$(echo "$ROOT_PUBLIC_KEY" | tr -d '\r')
fi

# 3. 是否禁止root SSH登录
DISABLE_ROOT_LOGIN="n"
read -p "是否禁止root用户SSH登录？[y/N]: " DISABLE_ROOT_LOGIN

# 4. 是否配置新用户公钥（仅当创建新用户时）
CONFIG_USER_KEY="n"
if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    read -p "是否配置新用户($NEW_USER)的SSH公钥？[y/N]: " CONFIG_USER_KEY

    if [[ "$CONFIG_USER_KEY" =~ ^[Yy]$ ]]; then
        echo ""
        echo -e "${YELLOW}请粘贴$NEW_USER用户的SSH公钥（粘贴完成后按回车结束）${NC}"
        read -r USER_PUBLIC_KEY
        USER_PUBLIC_KEY=$(echo "$USER_PUBLIC_KEY" | tr -d '\r')
    fi
fi

# 5. 是否禁止新用户密码登录（仅当创建新用户时）
DISABLE_USER_PASS="n"
if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    read -p "是否禁止$NEW_USER用户SSH密码登录（仅允许密钥登录）？[y/N]: " DISABLE_USER_PASS
fi

# ------------------- 1. 创建新用户 -------------------
if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}[步骤 1/7] 创建新管理员用户...${NC}"

    if id "$NEW_USER" &>/dev/null; then
        echo -e "  ${YELLOW}用户 '$NEW_USER' 已存在，更新密码${NC}"
        echo "$NEW_USER:$NEW_PASS" | chpasswd
    else
        useradd -m -s /bin/bash "$NEW_USER"
        if ! id "$NEW_USER" >/dev/null 2>&1; then
            echo -e "${RED}用户创建失败${NC}"
            exit 1
        fi
        usermod -aG sudo "$NEW_USER"
        if ! groups "$NEW_USER" | grep -q "\bsudo\b"; then
            echo -e "${RED}用户未成功加入sudo组${NC}"
            exit 1
        fi
        echo "$NEW_USER:$NEW_PASS" | chpasswd
        echo -e "  ${GREEN}✓ 用户 '$NEW_USER' 已创建并添加到sudo组${NC}"
    fi

    # 配置新用户公钥
    if [[ "$CONFIG_USER_KEY" =~ ^[Yy]$ ]]; then
        mkdir -p /home/$NEW_USER/.ssh
        chmod 700 /home/$NEW_USER/.ssh

        if [ -n "$USER_PUBLIC_KEY" ] && echo "$USER_PUBLIC_KEY" | grep -qE "^ssh-(rsa|ed25519|ecdsa)" 2>/dev/null; then
            echo "$USER_PUBLIC_KEY" > /home/$NEW_USER/.ssh/authorized_keys
            chmod 600 /home/$NEW_USER/.ssh/authorized_keys
            chown -R $NEW_USER:$NEW_USER /home/$NEW_USER/.ssh
            echo -e "  ${GREEN}✓ $NEW_USER 用户SSH公钥已配置${NC}"
            USER_KEY_STATUS="已配置"
        else
            echo -e "  ${RED}! 公钥格式不正确，已跳过密钥配置${NC}"
            USER_KEY_STATUS="配置失败（格式错误）"
        fi
    else
        mkdir -p /home/$NEW_USER/.ssh
        chmod 700 /home/$NEW_USER/.ssh
        USER_KEY_STATUS="未配置"
        echo -e "  ${YELLOW}! $NEW_USER 用户公钥未配置${NC}"
    fi
else
    echo ""
    echo -e "${BLUE}[步骤 1/7] 创建新管理员用户...${NC}"
    echo -e "  ${YELLOW}→ 跳过创建新用户${NC}"
fi

# ------------------- 2. 配置root公钥 -------------------
if [[ "$CONFIG_ROOT_KEY" =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}[步骤 2/7] 配置root用户SSH公钥...${NC}"

    mkdir -p /root/.ssh
    chmod 700 /root/.ssh

    if [ -n "$ROOT_PUBLIC_KEY" ] && echo "$ROOT_PUBLIC_KEY" | grep -qE "^ssh-(rsa|ed25519|ecdsa)" 2>/dev/null; then
        echo "$ROOT_PUBLIC_KEY" > /root/.ssh/authorized_keys
        chmod 600 /root/.ssh/authorized_keys
        echo -e "  ${GREEN}✓ root用户SSH公钥已配置${NC}"
        ROOT_KEY_STATUS="已配置"
    else
        echo -e "  ${RED}! 公钥格式不正确，已跳过密钥配置${NC}"
        ROOT_KEY_STATUS="配置失败（格式错误）"
    fi
else
    echo ""
    echo -e "${BLUE}[步骤 2/7] 配置root用户SSH公钥...${NC}"
    echo -e "  ${YELLOW}→ 跳过配置root公钥${NC}"
    ROOT_KEY_STATUS="未配置"
fi

# ------------------- 3. SSH 加固 -------------------
echo ""
echo -e "${BLUE}[步骤 3/7] 加固SSH配置...${NC}"

BACKUP_FILE="/etc/ssh/sshd_config.bak.$(date +%Y%m%d%H%M%S)"
cp /etc/ssh/sshd_config "$BACKUP_FILE"
echo -e "  ${GREEN}✓ 原配置已备份到: $BACKUP_FILE${NC}"

if [ "$CURRENT_SSH_PORT" = "22" ]; then
    NEW_SSH_PORT=$((RANDOM % 30000 + 30000))
    echo -e "  ${YELLOW}检测到当前SSH为默认端口22${NC}"
    read -p "  是否将SSH端口改为 $NEW_SSH_PORT ? [y/N]: " confirm
    if [[ "$confirm" =~ ^[Nn]$ || -z "$confirm" ]]; then
        NEW_SSH_PORT="22"
        echo -e "  ${YELLOW}→ 保留原端口 22${NC}"
    else
        if grep -qE "^\s*#?\s*Port\s+" /etc/ssh/sshd_config; then
            sed -i "s/^\s*#\?\s*Port\s.*/Port $NEW_SSH_PORT/" /etc/ssh/sshd_config
        else
            echo "Port $NEW_SSH_PORT" >> /etc/ssh/sshd_config
        fi
        echo -e "  ${GREEN}→ SSH端口已改为: $NEW_SSH_PORT${NC}"
    fi
else
    NEW_SSH_PORT="$CURRENT_SSH_PORT"
    echo -e "  ${GREEN}✓ 当前SSH已是高位端口: $NEW_SSH_PORT，保留${NC}"
fi

# 禁用root登录（如果用户选择）
if [[ "$DISABLE_ROOT_LOGIN" =~ ^[Yy]$ ]]; then
    sed -i 's/^\s*#\?\s*PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
    grep -qE "^\s*PermitRootLogin\s+" /etc/ssh/sshd_config || \
    echo "PermitRootLogin no" >> /etc/ssh/sshd_config
    echo -e "  ${GREEN}✓ 已禁用root SSH登录${NC}"
    ROOT_LOGIN_STATUS="已禁用"
else
    echo -e "  ${YELLOW}→ 保留root SSH登录${NC}"
    ROOT_LOGIN_STATUS="仍启用"
fi

grep -q "^PermitEmptyPasswords" /etc/ssh/sshd_config || \
echo "PermitEmptyPasswords no" >> /etc/ssh/sshd_config
grep -q "^MaxAuthTries" /etc/ssh/sshd_config || \
echo "MaxAuthTries 3" >> /etc/ssh/sshd_config
grep -q "^LoginGraceTime" /etc/ssh/sshd_config || \
echo "LoginGraceTime 30" >> /etc/ssh/sshd_config
grep -q "^ClientAliveInterval" /etc/ssh/sshd_config || \
echo "ClientAliveInterval 300" >> /etc/ssh/sshd_config
grep -q "^ClientAliveCountMax" /etc/ssh/sshd_config || \
echo "ClientAliveCountMax 2" >> /etc/ssh/sshd_config
grep -q "^X11Forwarding" /etc/ssh/sshd_config || \
echo "X11Forwarding no" >> /etc/ssh/sshd_config

# AllowUsers 配置
if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    if grep -qE "^\s*AllowUsers\s+" /etc/ssh/sshd_config; then
        if ! grep -qE "^\s*AllowUsers\s+.*\b$NEW_USER\b" /etc/ssh/sshd_config; then
            sed -i "s/^\s*AllowUsers\s+.*/& $NEW_USER/" /etc/ssh/sshd_config
        fi
    else
        echo "AllowUsers $NEW_USER" >> /etc/ssh/sshd_config
    fi
fi

sed -i 's/^\s*#\?\s*PubkeyAuthentication.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config
if ! grep -qE "^\s*PubkeyAuthentication\s+" /etc/ssh/sshd_config; then
    echo "PubkeyAuthentication yes" >> /etc/ssh/sshd_config
fi

# 禁用新用户密码登录（如果用户选择）
if [[ "$CREATE_USER" =~ ^[Yy]$ && "$DISABLE_USER_PASS" =~ ^[Yy]$ ]]; then
    if [ -f "/home/$NEW_USER/.ssh/authorized_keys" ] && [ -s "/home/$NEW_USER/.ssh/authorized_keys" ]; then
        sed -i 's/^\s*#\?\s*PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
        if ! grep -qE "^\s*PasswordAuthentication\s+" /etc/ssh/sshd_config; then
            echo "PasswordAuthentication no" >> /etc/ssh/sshd_config
        fi
        echo -e "  ${GREEN}✓ 已禁用SSH密码登录（仅允许密钥登录）${NC}"
        PASS_AUTH_STATUS="已禁用（密钥已配置）"
    else
        echo -e "${RED}! 未检测到有效SSH公钥，无法禁用密码登录${NC}"
        echo -e "${RED}  请先配置公钥后再禁用密码登录${NC}"
        PASS_AUTH_STATUS="禁用失败（未配置密钥）"
    fi
else
    echo -e "  ${YELLOW}→ 保留SSH密码登录${NC}"
    PASS_AUTH_STATUS="仍启用"
fi

echo "正在检查SSH配置..."

if ! sshd -t; then
    echo "=========================================="
    echo "SSH配置错误，正在恢复..."
    cp "$BACKUP_FILE" /etc/ssh/sshd_config
    systemctl restart ssh 2>/dev/null || systemctl restart sshd 2>/dev/null
    exit 1
fi

systemctl reload ssh 2>/dev/null \
|| systemctl reload sshd 2>/dev/null \
|| systemctl restart ssh 2>/dev/null \
|| systemctl restart sshd 2>/dev/null

sleep 2

if ! systemctl is-active ssh >/dev/null 2>&1 \
&& ! systemctl is-active sshd >/dev/null 2>&1; then
    echo "SSH启动失败，恢复配置..."
    cp "$BACKUP_FILE" /etc/ssh/sshd_config
    systemctl restart ssh 2>/dev/null || systemctl restart sshd
    exit 1
fi

echo -e "${BLUE}正在测试SSH新端口连通性...${NC}"
if timeout 5 bash -c "echo > /dev/tcp/localhost/$NEW_SSH_PORT"; then
    echo -e "  ${GREEN}✓ SSH端口 $NEW_SSH_PORT 测试成功${NC}"
else
    echo -e "${RED}[紧急] SSH端口 $NEW_SSH_PORT 不可用，恢复原配置...${NC}"
    cp "$BACKUP_FILE" /etc/ssh/sshd_config
    systemctl restart ssh 2>/dev/null || systemctl restart sshd
    exit 1
fi

# ------------------- 4. 安装依赖 -------------------
echo ""
echo -e "${BLUE}[步骤 4/7] 安装安全组件...${NC}"
wait_apt
apt-get update -qq
wait_apt
apt-get install -y -qq ufw fail2ban unattended-upgrades curl wget vim unzip zip htop git net-tools 2>/dev/null || \
apt-get install -y ufw fail2ban unattended-upgrades curl wget vim unzip zip htop git net-tools
echo -e "  ${GREEN}✓ UFW、Fail2ban、自动更新、常用工具已安装${NC}"

# ------------------- 5. 配置防火墙 -------------------
echo ""
echo -e "${BLUE}[步骤 5/7] 配置UFW防火墙...${NC}"

ufw allow "$NEW_SSH_PORT/tcp" >/dev/null 2>&1 || true

ufw --force reset >/dev/null 2>&1
ufw default deny incoming >/dev/null
ufw default allow outgoing >/dev/null
ufw allow "$NEW_SSH_PORT/tcp" >/dev/null

read -p "请输入需要开放的端口(默认80,443，可写80,443,8080): " CUSTOM_PORTS

[ -z "$CUSTOM_PORTS" ] && CUSTOM_PORTS="80,443"

IFS=',' read -ra PORTS <<< "$CUSTOM_PORTS"

for p in "${PORTS[@]}"; do
    ufw allow "$p/tcp" >/dev/null 2>&1 || true
done

ufw status verbose
ufw --force enable >/dev/null

echo -e "  ${GREEN}✓ 防火墙已启用，仅开放: SSH($NEW_SSH_PORT), TCP端口($CUSTOM_PORTS)${NC}"

# ------------------- 6. 配置Fail2ban -------------------
echo ""
echo -e "${BLUE}[步骤 6/7] 配置Fail2ban...${NC}"

if [ -f "/etc/fail2ban/jail.local" ]; then
    mv /etc/fail2ban/jail.local "/etc/fail2ban/jail.local.bak.$(date +%Y%m%d%H%M%S)"
fi

cat > /etc/fail2ban/jail.local <<EOF
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5
backend = systemd

[sshd]
enabled = true
port = $NEW_SSH_PORT
filter = sshd
backend = systemd
maxretry = 5
bantime = 3600
EOF

systemctl enable fail2ban --now 2>/dev/null || true
systemctl restart fail2ban 2>/dev/null || true
echo -e "  ${GREEN}✓ Fail2ban已启动: 5次错误密码 → 封禁1小时${NC}"

# ------------------- 7. 配置自动安全更新 -------------------
echo ""
echo -e "${BLUE}[步骤 7/7] 配置自动安全更新...${NC}"

if [ -f "/etc/apt/apt.conf.d/50unattended-upgrades" ]; then
    mv /etc/apt/apt.conf.d/50unattended-upgrades "/etc/apt/apt.conf.d/50unattended-upgrades.bak.$(date +%Y%m%d%H%M%S)"
fi

cat > /etc/apt/apt.conf.d/50unattended-upgrades <<'EOF'
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}-security";
    "${distro_id}ESMApps:${distro_codename}-apps-security";
    "${distro_id}ESM:${distro_codename}-infra-security";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::MinimalSteps "true";
Unattended-Upgrade::InstallOnShutdown "false";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Remove-New-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
EOF

if [ -f "/etc/apt/apt.conf.d/20auto-upgrades" ]; then
    mv /etc/apt/apt.conf.d/20auto-upgrades "/etc/apt/apt.conf.d/20auto-upgrades.bak.$(date +%Y%m%d%H%M%S)"
fi

cat > /etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
EOF

systemctl enable unattended-upgrades --now 2>/dev/null || true
echo -e "  ${GREEN}✓ 自动安全更新已启用（每天检查）${NC}"

# ------------------- 系统基础优化 -------------------
echo ""
echo -e "${BLUE}[步骤 8/8] 系统基础优化...${NC}"
if sysctl net.ipv4.tcp_available_congestion_control 2>/dev/null | grep -q bbr; then
    grep -q "tcp_congestion_control=bbr" /etc/sysctl.conf || echo "net.ipv4.tcp_congestion_control=bbr" >> /etc/sysctl.conf
    grep -q "default_qdisc=fq" /etc/sysctl.conf || echo "net.core.default_qdisc=fq" >> /etc/sysctl.conf
    sysctl -p >/dev/null 2>&1
    echo "✓ 已开启BBR"
fi

timedatectl set-timezone Asia/Shanghai 2>/dev/null || true

echo -e "  ${GREEN}✓ 时区设为Asia/Shanghai"

# ------------------- 保存配置信息 -------------------
INFO_FILE="/root/server-secure-info.txt"
cat > "$INFO_FILE" <<EOF
================================================================================
                    服务器安全加固配置报告
================================================================================
生成时间: $(date '+%Y-%m-%d %H:%M:%S')
服务器IP: ${PUBLIC_IP}
SSH端口:  ${NEW_SSH_PORT}
================================================================================

【用户配置选择】
EOF

if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    echo "新用户名: ${NEW_USER}" >> "$INFO_FILE"
    echo "新用户密码: ${NEW_PASS}" >> "$INFO_FILE"
    echo "新用户公钥: ${USER_KEY_STATUS:-未配置}" >> "$INFO_FILE"
    echo "新用户密码登录: ${PASS_AUTH_STATUS}" >> "$INFO_FILE"
else
    echo "新用户: 未创建" >> "$INFO_FILE"
fi

echo "root公钥: ${ROOT_KEY_STATUS:-未配置}" >> "$INFO_FILE"
echo "root SSH登录: ${ROOT_LOGIN_STATUS}" >> "$INFO_FILE"

cat >> "$INFO_FILE" <<EOF

================================================================================
【已完成的安全加固项】
================================================================================
EOF

if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    echo "[✓] 创建新用户: ${NEW_USER} (sudo权限)" >> "$INFO_FILE"
    echo "[✓] 新用户公钥: ${USER_KEY_STATUS:-未配置}" >> "$INFO_FILE"
else
    echo "[-] 创建新用户: 未执行" >> "$INFO_FILE"
fi

echo "[✓] root公钥: ${ROOT_KEY_STATUS:-未配置}" >> "$INFO_FILE"
echo "[✓] root SSH登录: ${ROOT_LOGIN_STATUS}" >> "$INFO_FILE"
echo "[✓] SSH端口: ${NEW_SSH_PORT}" >> "$INFO_FILE"
echo "[✓] SSH密码登录: ${PASS_AUTH_STATUS}" >> "$INFO_FILE"
echo "[✓] UFW防火墙: 仅开放 SSH(${NEW_SSH_PORT}) 和业务端口(${CUSTOM_PORTS})" >> "$INFO_FILE"
echo "[✓] Fail2ban: 5次错误密码尝试 → 自动封禁IP 1小时" >> "$INFO_FILE"
echo "[✓] 自动安全更新: 已启用（每日检查并安装安全补丁）" >> "$INFO_FILE"
echo "[✓] 常用工具: curl wget vim unzip zip htop git net-tools 已安装" >> "$INFO_FILE"
echo "[✓] 时区设置: Asia/Shanghai" >> "$INFO_FILE"
echo "[✓] 配置备份: ${BACKUP_FILE}" >> "$INFO_FILE"
echo "[✓] 执行日志: ${LOG_FILE}" >> "$INFO_FILE"

cat >> "$INFO_FILE" <<EOF

================================================================================
【重要提醒】
================================================================================
1. 如果SSH端口从22改成了 ${NEW_SSH_PORT}，以后连接必须带端口参数:
EOF

if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    echo "   ssh -p ${NEW_SSH_PORT} ${NEW_USER}@${PUBLIC_IP}" >> "$INFO_FILE"
else
    echo "   ssh -p ${NEW_SSH_PORT} root@${PUBLIC_IP}" >> "$INFO_FILE"
fi

cat >> "$INFO_FILE" <<EOF

2. 如果配置了密钥登录，请测试密钥登录是否正常。

3. 如果禁用了密码登录，请确保密钥登录已正常工作。

4. 建议定期查看安全日志:
   sudo fail2ban-client status sshd
   sudo ufw status verbose

5. 数据备份建议:
   - 重要数据定期备份到本地或其他云存储
   - 可利用云服务商后台快照功能做系统级备份

================================================================================
EOF

chmod 600 "$INFO_FILE"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}              ${CYAN}🎉 安全加固全部完成！${NC}                       ${GREEN}║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${YELLOW}📄 所有配置信息已保存到:${NC} ${CYAN}${INFO_FILE}${NC}"
echo -e "  ${YELLOW}📄 执行日志已保存到:${NC} ${CYAN}${LOG_FILE}${NC}"
echo -e "  ${YELLOW}📄 请立即查看:${NC} ${CYAN}cat ${INFO_FILE}${NC}"
echo ""
echo -e "  ${RED}⚠️  重要提醒:${NC}"
echo -e "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [[ "$CREATE_USER" =~ ^[Yy]$ ]]; then
    echo -e "  1. 新用户: ${GREEN}${NEW_USER}${NC}"
    echo -e "  2. SSH端口: ${GREEN}${NEW_SSH_PORT}${NC}"
    echo -e "  3. 连接命令: ${GREEN}ssh -p ${NEW_SSH_PORT} ${NEW_USER}@${PUBLIC_IP}${NC}"
    echo -e "  4. ${RED}请立即用新用户登录测试，确认正常后再关闭root会话${NC}"
else
    echo -e "  1. SSH端口: ${GREEN}${NEW_SSH_PORT}${NC}"
    echo -e "  2. 连接命令: ${GREEN}ssh -p ${NEW_SSH_PORT} root@${PUBLIC_IP}${NC}"
fi
echo ""
echo -e "  ${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""