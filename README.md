# HDSLedger - Group 26

## Introduction

HDSLedger is a simplified permissioned (closed membership) blockchain system with high dependability
guarantees. It uses the Istanbul BFT consensus algorithm to ensure that all nodes run commands
in the same order, achieving State Machine Replication (SMR) and guaranteeing that all nodes
have the same state.

## Table of Contents

1. [Introduction](#introduction)
2. [Run IBFT](#run-IBFT)
3. [Tests](#tests)
2. [Requirements](#requirements)
3. [Configuration Files](#configuration-files)
   - [Node configuration](#node-configuration)
4. [Dependencies](#dependencies)
5. [Puppet Master](#puppet-master)
   - [Running the Script](#running-the-script)
6. [Maven](#maven)
   - [Installation](#installation)
   - [Execution](#execution)
7. [Tests](#tests)
8. [Acknowledgements](#acknowledgements)

## Run IBFT

Both the tests and IBFT application can be ran using:

```bash
sudo python3 puppet-master.py
```

The setup and requirements are the same as the provided setup in the inital zip-folder. The tests are integrated and will be automatically ran when running this command. To interact with the application find the the client1 shell and enter append "some_value". This will simulate normal behaviour from the system. 

The main functionality of the system is the transfer and balance methods from clients. We have three clients; client1, client2 and client3. to transfer money from one to another enter in the client's terminal: 

```sh
transfer <amount> <destination>
```
To check the client's own balance:

```sh
balance
```
To check someone else's balance:

```sh
check <user>
```

## Tests

The tests are a part of the maven build and run automatically when you use puppet master to run the project. 

You will see the output of the tests under a test-banner in your terminal.
```sh
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
```

Tests can be hard to find, as the shutdown of clients and nodes instances is not done cleanly, leading to the program throwing errors. However, the tests will run. Look for the green text in the terminal, which will indicate that the tests are complete and executed correctly. 

The tests can be found in under `Service/src/test/`. 

To only run the test enter:

```sh
sudo mvn clean test
```

For more information about the tests and their purpose, take a look in the report. 

## Requirements

- [Java 17](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) - Programming language;

- [Maven 3.8](https://maven.apache.org/) - Build and dependency management tool;

- [Python 3](https://www.python.org/downloads/) - Programming language;

---

# Configuration Files

### Node configuration

Can be found inside the `resources/` folder of the `Service` module.

```json
{
    "id": <NODE_ID>,
    "isLeader": <IS_LEADER>,
    "hostname": "localhost",
    "port": <NODE_PORT>,
}
```

Configuration for the client node(s) can also be found in the `resources/` folder of the `Service` module. 
```json
{
    "id": <CLIENT_ID>,
    "isLeader": <IS_LEADER>,
    "hostname": "localhost",
    "port": <CLIENT_PORT>,
}
```

## Dependencies

To install the necessary dependencies run the following command:

```bash
./install_deps.sh
```

This should install the following dependencies:

- [Google's Gson](https://github.com/google/gson) - A Java library that can be used to convert Java Objects into their JSON representation.

## Puppet Master

The puppet master is a python script `puppet-master.py` which is responsible for starting the nodes
of the blockchain.
The script runs with `kitty` terminal emulator by default since it's installed on the RNL labs.

To run the script you need to have `python3` installed.
The script has arguments which can be modified:

- `terminal` - the terminal emulator used by the script
- `server_config` - a string from the array `server_configs` which contains the possible configurations for the blockchain nodes

Run the script with the following command:

```bash
python3 puppet-master.py
```
Note: You may need to install **kitty** in your computer

## Maven

It's also possible to run the project manually by using Maven.

### Installation

Compile and install all modules using:

```
mvn clean install
```

### Execution

Run without arguments

```
cd <module>/
mvn compile exec:java
```

Run with arguments

```
cd <module>/
mvn compile exec:java -Dexec.args="..."
```

## Acknowledgements
This codebase was adapted from last year's project solution, which was kindly provided by the following group: [David Belchior](https://github.com/DavidAkaFunky), [Diogo Santos](https://github.com/DiogoSantoss), [Vasco Correia](https://github.com/Vaascoo). We thank all the group members for sharing their code.

## Feedbacks
- Clean up in general, but especially the NodeService class. Make layers of abstraction, move to other classes.
- Add more tests for Byzantine Leaders
- Need to send the whole message in some extra function, not just ROUND CHANGE
