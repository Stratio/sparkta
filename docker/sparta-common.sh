#!/bin/bash

function loadVariables() {
 source "${VARIABLES}"
 source "${SYSTEM_VARIABLES}"
}

function initSpark() {
  if [[ ! -v SPARK_HOME ]]; then
    SPARK_HOME="/opt/spark/dist"
  fi
  if [[ ! -v SPARK_ENV_FILE ]]; then
    SPARK_ENV_FILE="${SPARK_HOME}/conf/spark-env.sh"
  fi
  if [[ ! -v SPARK_CONF_DEFAULTS_FILE ]]; then
    SPARK_CONF_DEFAULTS_FILE="${SPARK_HOME}/conf/spark-defaults.conf"
  fi
  if [[ ! -v SPARK_CONF_LOG_FILE ]]; then
    cp "${SPARK_HOME}/conf/log4j.properties.template" "${SPARK_HOME}/conf/log4j.properties"
    SPARK_CONF_LOG_FILE="${SPARK_HOME}/conf/log4j.properties"
  fi
  echo "" >> ${VARIABLES}
  echo "export SPARK_HOME=${SPARK_HOME}" >> ${VARIABLES}
  echo "" >> ${SYSTEM_VARIABLES}
  echo "export SPARK_HOME=${SPARK_HOME}" >> ${SYSTEM_VARIABLES}
  echo "" >> ${VARIABLES}
  echo "export SPARK_ENV_FILE=${SPARK_ENV_FILE}" >> ${VARIABLES}
  echo "" >> ${SYSTEM_VARIABLES}
  echo "export SPARK_ENV_FILE=${SPARK_ENV_FILE}" >> ${SYSTEM_VARIABLES}
  echo "export SPARK_CONF_DEFAULTS_FILE=${SPARK_CONF_DEFAULTS_FILE}" >> ${VARIABLES}
  echo "" >> ${SYSTEM_VARIABLES}
  echo "export SPARK_CONF_DEFAULTS_FILE=${SPARK_CONF_DEFAULTS_FILE}" >> ${SYSTEM_VARIABLES}
  echo "export SPARK_CONF_LOG_FILE=${SPARK_CONF_LOG_FILE}" >> ${VARIABLES}
  echo "" >> ${SYSTEM_VARIABLES}
  echo "export SPARK_CONF_LOG_FILE=${SPARK_CONF_LOG_FILE}" >> ${SYSTEM_VARIABLES}

  rm "${SPARK_HOME}/jars/curator-client-2.6.0.jar"
  rm "${SPARK_HOME}/jars/curator-recipes-2.6.0.jar"
  rm "${SPARK_HOME}/jars/curator-framework-2.6.0.jar"
  rm "${SPARK_HOME}/jars/zookeeper-3.4.6.jar"

}

function initSparkEnvOptions() {

  if [ -v CALICO_ENABLED ] && [ $CALICO_ENABLED == "true" ] && [ -v CALICO_NETWORK ] && [ ${#CALICO_NETWORK} != 0 ]; then
    HOST="$(hostname --all-ip-addresses|xargs)"
    echo "Virutal network detected changed LIBPROCESS_IP $LIBPROCESS_IP to $HOST"
    export LIBPROCESS_IP=$HOST
  fi

 # if [ -v PORT_SPARKUI ] && [ ${#PORT_SPARKUI} != 0 ]; then
 #  sed -i "s|.*SPARK_MASTER_WEBUI_PORT.*|SPARK_MASTER_WEBUI_PORT=${PORT_SPARKUI}|" ${SPARK_ENV_FILE}
 # fi

}

function initHdfs() {
  if [[ -v HADOOP_USER_NAME ]]; then
    echo "" >> ${VARIABLES}
    echo "export HADOOP_USER_NAME=${HADOOP_USER_NAME}" >> ${VARIABLES}
    echo "" >> ${SYSTEM_VARIABLES}
    echo "export HADOOP_USER_NAME=${HADOOP_USER_NAME}" >> ${SYSTEM_VARIABLES}
  fi

  if [[ ! -v HADOOP_CONF_FROM_URI ]]; then
   HADOOP_CONF_FROM_URI="false"
  fi
  if [ $HADOOP_CONF_FROM_URI == "true" ] && [ -v HADOOP_CONF_URI ] && [ ${#HADOOP_CONF_URI} != 0 ]; then
    if [ ! -v HADOOP_CONF_DIR ] && [ ${#HADOOP_CONF_DIR} != 0 ]; then
      HADOOP_CONF_DIR=/opt/sds/hadoop/conf
    fi
    sed -i "s|.*sparta.hdfs.hdfsMaster.*|#sparta.hdfs.hdfsMaster = \""${HADOOP_CONF_URI}"\"|" ${SPARTA_CONF_FILE}
    source hdfs_utils.sh
    generate_hdfs-conf-from-uri
  fi

  if [[ ! -v CORE_SITE_FROM_URI ]]; then
   CORE_SITE_FROM_URI="false"
  fi
  if [ $CORE_SITE_FROM_URI == "true" ] && [ -v HADOOP_CONF_URI ] && [ ${#HADOOP_CONF_URI} != 0 ]; then
    if [ ! -v HADOOP_CONF_DIR ] && [ ${#HADOOP_CONF_DIR} != 0 ]; then
      HADOOP_CONF_DIR=/opt/sds/hadoop/conf
    fi
    sed -i "s|.*sparta.hdfs.hdfsMaster.*|#sparta.hdfs.hdfsMaster = \""${HADOOP_CONF_URI}"\"|" ${SPARTA_CONF_FILE}
    source hdfs_utils.sh
    generate_core-site-from-uri
  fi

  if [[ ! -v HADOOP_CONF_FROM_DFS ]]; then
   HADOOP_CONF_FROM_DFS="false"
  fi
  if [ $HADOOP_CONF_FROM_DFS == "true" ] && [ -v HADOOP_FS_DEFAULT_NAME ] && [ ${#HADOOP_FS_DEFAULT_NAME} != 0 ]; then
    if [ ! -v HADOOP_CONF_DIR ] && [ ${#HADOOP_CONF_DIR} != 0 ]; then
      HADOOP_CONF_DIR=/opt/sds/hadoop/conf
    fi
    sed -i "s|.*sparta.hdfs.hdfsMaster.*|#sparta.hdfs.hdfsMaster = \""${HADOOP_FS_DEFAULT_NAME}"\"|" ${SPARTA_CONF_FILE}
    source hdfs_utils.sh
    generate_hdfs-conf-from-fs
  fi

  if [[ ! -v HADOOP_CONF_FROM_DFS_NOT_SECURED ]]; then
   HADOOP_CONF_FROM_DFS_NOT_SECURED="false"
  fi
  if [ $HADOOP_CONF_FROM_DFS_NOT_SECURED == "true" ] && [ -v HADOOP_FS_DEFAULT_NAME ] && [ ${#HADOOP_FS_DEFAULT_NAME} != 0 ]; then
    if [ ! -v HADOOP_CONF_DIR ] && [ ${#HADOOP_CONF_DIR} != 0 ]; then
      HADOOP_CONF_DIR=/opt/sds/hadoop/conf
    fi
    sed -i "s|.*sparta.hdfs.hdfsMaster.*|#sparta.hdfs.hdfsMaster = \""${HADOOP_FS_DEFAULT_NAME}"\"|" ${SPARTA_CONF_FILE}
    source hdfs_utils.sh
    generate_hdfs-conf-from-fs-not-secured
  fi
}

function logLevelOptions() {
  if [[ ! -v SERVICE_LOG_LEVEL ]]; then
    SERVICE_LOG_LEVEL="ERROR"
  fi
  sed -i "s|<root level.*|<root level = \""${SERVICE_LOG_LEVEL}"\">|" ${LOG_CONFIG_FILE}
  sed -i "s|log4j.appender.console.layout.ConversionPattern.*|log4j.appender.console.layout.ConversionPattern=%d{dd MMM YYYY HH:mm:ss.SSS}\t%p\t%c{35}\t%m%n|" ${SPARK_CONF_LOG_FILE}

  if [[ ! -v SPARTA_LOG_LEVEL ]]; then
    SPARTA_LOG_LEVEL="INFO"
  fi
  sed -i "s|com.stratio.sparta.*|com.stratio.sparta\" level= \""${SPARTA_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  echo "" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.com.stratio.sparta=${SPARTA_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}

  if [[ ! -v CROSSDATA_LOG_LEVEL ]]; then
      CROSSDATA_LOG_LEVEL="ERROR"
  fi
  sed -i "s|com.stratio.crossdata.*|com.stratio.crossdata\" level= \""${CROSSDATA_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  echo "" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.com.stratio.crossdata=${CROSSDATA_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}

  if [[ ! -v SPARK_LOG_LEVEL ]]; then
    SPARK_LOG_LEVEL="ERROR"
  fi
  sed -i "s|org.apache.spark.*|org.apache.spark\" level= \""${SPARK_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  sed -i "s|org.apache.spark-project.*|org.apache.spark-project\" level= \""${SPARK_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  sed -i "s|log4j.rootCategory.*|log4j.rootCategory= ${SPARK_LOG_LEVEL}, console|" ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.org.apache.spark=${SPARK_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.org.spark-project=${SPARK_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}

  if [[ ! -v HADOOP_LOG_LEVEL ]]; then
    HADOOP_LOG_LEVEL="ERROR"
  fi
  sed -i "s|org.apache.hadoop.*|org.apache.hadoop\" level= \""${HADOOP_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  echo "" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.org.apache.hadoop=${HADOOP_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}

  if [[ ! -v ZOOKEEPER_LOG_LEVEL ]]; then
    ZOOKEEPER_LOG_LEVEL="ERROR"
  fi
  sed -i "s|org.apache.zookeeper.ClientCnxn.*|org.apache.zookeeper.ClientCnxn\" level= \""${ZOOKEEPER_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  sed -i "s|org.I0Itec.zkclient.*|org.I0Itec.zkclient\" level= \""${ZOOKEEPER_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  echo "" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.org.apache.zookeeper=${ZOOKEEPER_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.org.I0Itec.zkclient=${ZOOKEEPER_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}

  if [[ ! -v PARQUET_LOG_LEVEL ]]; then
    PARQUET_LOG_LEVEL="ERROR"
  fi
  sed -i "s|org.apache.parquet.*|org.apache.parquet\" level= \""${PARQUET_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  echo "" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.org.apache.parquet=${PARQUET_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}

  if [[ ! -v AVRO_LOG_LEVEL ]]; then
    AVRO_LOG_LEVEL="ERROR"
  fi
  sed -i "s|org.apache.avro.*|org.apache.avro\" level= \""${AVRO_LOG_LEVEL}"\"/>|" ${LOG_CONFIG_FILE}
  echo "" >> ${SPARK_CONF_LOG_FILE}
  echo "log4j.logger.org.apache.avro=${AVRO_LOG_LEVEL}" >> ${SPARK_CONF_LOG_FILE}
}

function logLevelToStdout() {
  SERVICE_LOG_APPENDER="STDOUT"
  export SPARTA_OPTS="$SPARTA_OPTS -Dconfig.file=$SPARTA_CONF_FILE"
  sed -i "s|<appender-ref ref.*|<appender-ref ref= \""${SERVICE_LOG_APPENDER}"\" />|" ${LOG_CONFIG_FILE}
}

function logLevelToFile() {
  SERVICE_LOG_APPENDER="FILE"
     sed -i "s|<appender-ref ref.*|<appender-ref ref= \""${SERVICE_LOG_APPENDER}"\" />|" ${LOG_CONFIG_FILE}
}
