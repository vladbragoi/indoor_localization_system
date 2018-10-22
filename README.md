# Fingerprinting (Android application)

This repository contains an Android application than can be used to build an indoor localization system.

## CouchDB server setup
First, you need to install and configure your own instance of CouchDB on a remote computer/server.

Follow CouchDB official instructions [here](docs.couchdb.org/en/latest/install/unix.html
) and make sure to let CouchDB listen any available IP address, using:

```
[chttpd]
bind_address = 0.0.0.0
```

in the configuration file (`server_ip:5984/_utils/`, *Configuration* tab on the left menu).

## Python server setup
Follow the instructions [here](https://github.com/vladbragoi/fingerprinting_server) for configuring and starting the server application.

## Application setup
Fisrt, Download and install the Android application, then start it, allow permissions and open the *Settings* menu on the right.
Here you need to specify:
* Your name
* The Fingerprinting database parameters:
  * Database Name
  * Database URL (that needs to coincide with your remote computer/server, and need to be in the form of `http://server_ip:5984/`)
  * Database Username (generally `admin`, but for better security follow [these](http://docs.couchdb.org/en/stable/intro/security.html?highlight=user) instructions)
  * Database Password
* The Localization database parameters:
  * Database Name (that need to be different from the fingerprinting database name)
  * Database URL (that could be the same ip of the fingerprinting database)
  * Database Username (same as above)
  * Database Password
 
 *Note that the Debug option should be disabled or it will work only with access points of the University of Verona*
 
 ## Application usage
 
