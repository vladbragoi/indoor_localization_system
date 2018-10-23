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
Launch ```./server.py``` if you want to start the *Localization service*.

Use the ```./converter.py``` if you want to convert a document from old JSON format to new one, as indicated in the script.


## Terms and Licence
This software is distributed under Apache-2.0 license. See [LICENSE](LICENSE) for details.