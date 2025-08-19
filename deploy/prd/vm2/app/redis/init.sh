# redis 主从容器创建好后 进入一个容器执行的命令，注意节点ip地址

vm1ip=你的云服务器1内网ip/你的虚拟机1内网ip
vm2ip=你的云服务器2内网ip/你的虚拟机2内网ip

redis-cli --cluster create ${vm1ip}:6380 ${vm1ip}:6381 ${vm1ip}:6382 ${vm2ip}:6383 ${vm2ip}:6384 ${vm2ip}:6385 --cluster-replicas 1 -a Hf@173503494 --cluster-yes
