@Library('libpipelines@master') _

hose {
    EMAIL = 'sparta'
    MODULE = 'sparta-workflow'
    DEVTIMEOUT = 70
    RELEASETIMEOUT = 40
    FOSS = true
    REPOSITORY = 'sparta-workflow'
    PKGMODULES = ['dist']
    PKGMODULESNAMES = ['stratio-sparta']
    DEBARCH = 'all'
    RPMARCH = 'noarch'
    EXPOSED_PORTS = [9090]
    KMS_UTILS = '0.2.1'
    BASEIMG = 'qa.stratio.com/stratio/stratio-spark:2.1.0.1'
    DOCKERFILECOMMAND = 'WORKDIR / \n RUN apt-get update -y && apt-get install -y krb5-user libpam-krb5 libpam-ccreds auth-client-config curl wget php5-curl make jq vim && update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java && curl https://www.openssl.org/source/openssl-1.0.2l.tar.gz | tar xz && cd openssl-1.0.2l && sudo ./config && sudo make && sudo make install && sudo ln -sf /usr/local/ssl/bin/openssl /usr/bin/openssl && wget https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64 && chmod +x jq-linux64 && mv jq-linux64 /usr/bin/jq'

    ITSERVICES = [
            ['RABBITMQ': [
               'image': 'rabbitmq:3-management'
            ]],
          ]

    ITPARAMETERS = "-Drabbitmq.hosts=%%RABBITMQ"


    DEV = { config ->
    
        doCompile(config)

        parallel(UT: {
            doUT(config)
        }, IT: {
            doIT(config)
        }, failFast: config.FAILFAST)

        doPackage(config)

        parallel(DOC: {
            doDoc(config)
        }, QC: {
            doStaticAnalysis(config)
        }, DEPLOY: {
            doDeploy(config)
        }, DOCKER : {    
            doDocker(config)
        }, failFast: config.FAILFAST)

    }
    INSTALLSERVICES = [
        ['CHROME': [
            'image': 'stratio/selenium-chrome:48',
            'volumes': ['/dev/shm:/dev/shm'],
            'env': ['SELENIUM_GRID=selenium.cd','ID=%%JUID']
            ]
        ],
        ['DCOSCLI': [
                'image': 'stratio/dcos-cli:0.4.15',
                'volumes': ['stratio/paasintegrationpem:0.1.0'],
                'env': [
                    'DCOS_IP=10.200.0.205',
                    'SSL=true',
                    'SSH=true',
                    'TOKEN_AUTHENTICATION=true',
                    'DCOS_USER=admin@demo.stratio.com',
                    'DCOS_PASSWORD=stratiotest',
                    'BOOTSTRAP_USER=operador',
                    'PEM_FILE_PATH=/paascerts/PaasIntegration.pem'
                    ],
                'sleep':  10
                ]
        ]  
    ]
    INSTALLPARAMETERS = """
            | -DSTRATIO_SPARTA_VERSION=1.7.4
            | -DDOCKER_URL=qa.stratio.com/stratio/sparta
            | -DDCOS_SERVICE_NAME=sparta-server
            | -DFORCEPULLIMAGE=false    
            | -DZK_URL=zk-0001-zookeeperstable.service.paas.labs.stratio.com:2181,zk-0002-zookeeperstable.service.paas.labs.stratio.com:2181,zk-0003-zookeeperstable.service.paas.labs.stratio.com:2181
            | -DMARATHON_SSO_CLIENT_ID=adminrouter_paas-master-1.node.paas.labs.stratio.com
            | -DHDFS_IP=10.200.0.74
            | -DHDFS_PORT=8020
            | -DHDFS_REALM=DEMO.STRATIO.COM
            | -DCROSSDATA_SERVER_CONFIG_SPARK_IMAGE=qa.stratio.com/stratio/stratio-spark:2.1.0.1
            | -DROLE_SPARTA=open
            | -DID_POLICY_ZK=zk_sparta
            | -DDCOS_CLI_HOST=%%DCOSCLI#0
            """
    INSTALL = { config ->
        doAT(conf: config, groups: ['dcos_instalation'])
     }
}
