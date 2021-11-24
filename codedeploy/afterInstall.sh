sudo systemctl stop tomcat9

#sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/home/ubuntu/webapp/cloudwatch-agent.json -s
#sudo systemctl restart amazon-cloudwatch-agent

#sudo chmod 777 /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar


# sudo mv /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar /home/ubuntu/webapp

sudo mv /home/ubuntu/webapp/webapp.service  /etc/systemd/system

sudo systemctl daemon-reload


