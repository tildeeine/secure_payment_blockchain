# HDSLedger - Group 26

## Introduction

HDSLedger is a simplified permissioned (closed membership) blockchain system with high dependability
guarantees. It uses the Istanbul BFT consensus algorithm to ensure that all nodes run commands
in the same order, achieving State Machine Replication (SMR) and guarantees that all nodes
have the same state.

## Table of Contents

1. [Introduction](#introduction)
2. [Requirements](#requirements)
3. [Configuration Files](#configuration-files)
   - [Node configuration](#node-configuration)
4. [Dependencies](#dependencies)
5. [Puppet Master](#puppet-master)
   - [Running the Script](#running-the-script)
6. [Maven](#maven)
   - [Installation](#installation)
   - [Execution](#execution)
7. [Demo Applications/Tests](#demo-applications-and-tests)
   - [How to Run](#how-to-run)
   - [Byzantine Behavior Simulation](#byzantine-behavior-simulation)
     - [Test 1: [Name]](#test-1-name)
     - [Test 2: [Name]](#test-2-name)
   - [Running Demo Applications](#running-demo-applications)
     - [Demo App 1: [Name]](#demo-app-1-name)
     - [Demo App 2: [Name]](#demo-app-2-name)
8. [Acknowledgements](#acknowledgements)


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

## Demo Applications/Tests

### How to Run

We test:
- a non-leader sending startConsensus
- a non-leader sending pre-prepare
- sending a startConsensus for a round that has already been decided

We should test:
- Node sending conflicting pre-prepare messages for the same consensus instance and round (Double voting)
- Nodes sending conflicting commit messages for the same consensus instance
- Node sending messages with incorrect or malformed formats to disrupt the message processing
- Nodes sending messages with incorrect or forged signatures
- Node attempting to propose an incorrect value for a consensus instance
- Nodes executing commands out of order to disrupt the expected sequence

We could test:
- Nodes sending messages out-of-order or with significant delays (handling asynchrony)
- Nodes attempting to initiate consensus for a non-sequential round, violating the expected order
- Nodes flooding the network with a large number of messages (congestion)
- - Nodes working together to disrupt the consensus by sending conflicting messages together
- - Nodes refusing to particiapte in the consensus process or respond to messages (testing against uncooperative nodes)


1. Prerequisites: List any prerequisites or dependencies that need to be installed.
2. Setup: Instructions for setting up the project before running tests or demo applications.
3. Running Tests: Command or steps to execute JUnit tests.
4. Running Demo Applications: Command or steps to run the demo applications.

### Byzantine Behavior Simulation

#### Test 1: [Name]
- Description: Brief description of the test.
- Purpose: Explain the purpose of this test in demonstrating Byzantine behavior handling.
- Instructions: Step-by-step instructions on how to run the test.
- Expected Outcome: Describe the expected behavior of the system.

#### Test 2: [Name]
- ...

### Running Demo Applications

#### Demo App 1: [Name]
- Description: Brief description of the demo application.
- Purpose: Explain the purpose of this demo application in showcasing security features.
- Instructions: Step-by-step instructions on how to run the demo application.

#### Demo App 2: [Name]
- ...


## Acknowledgements
This codebase was adapted from last year's project solution, which was kindly provided by the following group: [David Belchior](https://github.com/DavidAkaFunky), [Diogo Santos](https://github.com/DiogoSantoss), [Vasco Correia](https://github.com/Vaascoo). We thank all the group members for sharing their code.

