@rest
Feature: [SPARTA][DCOS] Add sparta policy in gosec

  Background: Setup token to gosec
    #Generate token to conect to gosec
    Given I set sso token using host '${GOSECMANAGEMENT_HOST}' with user '${USER_NAME}' and password '${PASS_WORD}'
    And I securely send requests to '${GOSECMANAGEMENT_HOST}:${GOSECMANAGEMENT_PORT}'

  Scenario: [SPARTA][Gosec]Add zookeper-sparta policy to write in zookeper
    Given I send a 'POST' request to '${BASE_END_POINT}/api/policy' based on 'schemas/gosec/zookeeper_policy.json' as 'json' with:
      |   $.id                    |  UPDATE    | ${ID_POLICY_ZK}          | n/a    |
      |   $.name                  |  UPDATE    | ${NAME_POLICY_ZK}        | n/a |
    Then the service response status must be '201'
 
  Scenario: [SPARTA][Gosec]Add sparta policy to write in zookeper
    Given I send a 'POST' request to '${BASE_END_POINT}/api/policy' based on 'schemas/gosec/sp_policy.json' as 'json' with:
      |   $.id                    |  UPDATE    | ${ID_POLICY_SP}          | n/a |
      |   $.name                  |  UPDATE    | ${NAME_POLICY_SP}        | n/a |
    Then the service response status must be '201'

  Scenario: [SPARTA][Gosec]Add sparta policy to kafka
    Given I send a 'POST' request to '${BASE_END_POINT}/api/policy' based on 'schemas/gosec/kafka_policy.json' as 'json' with:
      |   $.id                    |  UPDATE    | ${ID_POLICY_KAFKA          | n/a |
      |   $.name                  |  UPDATE    | ${NAME_POLICY_KAFKA}        | n/a |
    Then the service response status must be '201'

# Example of execution with mvn :
#mvn verify -DBASE_END_POINT=/service/gosecmanagement -DGOSECMANAGEMENT_HOST='megadev.labs.stratio.com' -DGOSECMANAGEMENT_PORT=443 -Dit.test=com.stratio.sparta.testsAT.automated.dcos.installations.ISGosec_AddzookeperPolitic -DlogLevel=DEBUG -DUSER_NAME=admin -DPASS_WORD=1234 -DSECURED=true -Dmaven.failsafe.debu -DID_POLICY_ZK='sparta_zk' -DNAME_POLICY_ZK='zk_sparta' -DID_POLICY_SP='sparta' -DNAME_POLICY_SP='sparta' -DID_POLICY_KAFKA='kafka-sec' -DNAME_POLICY_KAFKA='kafka-sec'