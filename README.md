# Localization service

## Configuration file
Enter `config.ini` and modify the following fields:
* url: db url (could be `http://localhost:5984`)
* username: db username
* password: db password
* localization_db: the name of the localization database instance
(do not use the same db name of the fingerprinting one because there will be a lot of conflicts in the 
synchronized documents)
* fingerprinting_db: the name of the fingerprinting database instance
* fingerprint_size: the size of the reference point side
* default_direction: not used
* ap5ghz: a list of MAC addresses about Access Points (AP) that covers the environment with frequencies at 5GHz
* ap24ghz: a list of MAC addresses about AP that covers the environment with frequencies at 2.4GHz

### Start the server
Launch ```python3 server.py``` or ```./server.py``` if you want to start the *Localization service*.

Parameters you could specify when launching the server:
```
usage: server.py [-h] [-m INT-VALUE] [-i INPUT-FILE] [-o OUTPUT-FILE]
```
* -h: to display a help message
* -m: to specify the mode you want to use for the machine learning algorithm:
    * 2 (the default option), where both RSSI and MV are used to create a single classifier
    * 1 where both RSSI and MV are used to create a separated classifier
* -i: to specify an input file (need to be in JSON format) where to load the graph from
* -o: to specify if you want to export the graph and where 
(it will export a OUTPUT-FILE.json with graph parameters, and a OUTPUT-FILE.dot with dot notation for Graphviz visualizer).

Use the ```./converter.py``` if you want to convert a document from old JSON template to new one.
**Old JSON template:**
```json
{
    "_id": "x",
    "_rev": "xxx",
    "Misurazioni": {
        "NORTH": {
            "Misurazione 0": {
                "RSSI 0": {
                "id": "34:a8:4e:70:cd:1f",
                "value": -84
                },
                ...
                "RSSI N": {<->}
            }, 
            ...
            "Misurazione M": {<->}
        },
        "SOUTH": {<->},
        "EAST": {<->},
        "WEST": {<->}
    },
    "X position": "1",
    "Borders": "N, W",
    "Y position": "1"
}
```

**New JSON template:**
```json
{
    "_id": "x",
    "_rev": "xxx",
    "borders": "0",
    "x": "1",
    "y": "3",
    "measures": {
    "NORTH": {
      "wifi": [
        [
          {
            "id": "34:db:fd:a4:cd:0e",
            "value": -45,
            "type": "WIFI"
          },
          ...,
          {<->}
        ],
        [<->]
      ],
      "ble": [<->],
      "mv": [
        {
          "values": [
            -21.440125,
            0.29144287,
            -39.4516
          ]
        },
        {<->}
      ]
    },
    "SOUTH": {<->},
    "EAST": {<->},
    "WEST": {<->}
}
```

Parameters for the converter script:
```
usage: converter.py [-h] [-u URL] [-U USER] [-p PASSWORD]
```
* -h: display a help message
* -u: specify the url of the remote database
* -U: specify a username
* -p: specify a password

### Dev notes
The graph model usage need to be improved. You can modify the `loop` method in the `server.py` script if you have a 
cleverer mode of using it.

### Dependencies
* [cloudant](https://python-cloudant.readthedocs.io/en/latest/index.html)
* [matlab](https://it.mathworks.com/help/matlab/matlab-engine-for-python.html)
* [networkx](https://networkx.github.io/)
* matplotlib
* numpy


## Terms and Licence
This software is distributed under Apache-2.0 license. See [LICENSE](LICENSE) for details.