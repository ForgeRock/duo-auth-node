<!--
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright ${data.get('yyyy')} ForgeRock AS.
-->
# Duo Node

A Duo integration for ForgeRock's Identity Platform 7.0 and ForgeRock Identity Cloud. This integration handles:
1. Registration of the users device
2. Second factor authentication
3. Device Management (if applicable) 

**Installation**

1. Download the latest version of the Duo integration from [here](https://github.com/ForgeRock/duo-auth-node/releases/latest).
2. Copy the .jar file into the ../web-container/webapps/openam/WEB-INF/lib directory where AM is deployed.
3. Restart the web container to pick up the new node.  The node will then appear in the authentication trees components palette.


**Duo Configuration**
1. Create a Duo Account at https://signup.duo.com/.
2. Log in to the Duo Admin console and click on the 'Applications' tab.
![alt text](https://raw.githubusercontent.com/ForgeRock/duo-auth-node/master/images/Duo1.png "Duo Configuration 1")
3. Click 'Protect an Application'.
4. In the search bar type in 'Web SDK'.
![alt text](https://raw.githubusercontent.com/ForgeRock/duo-auth-node/master/images/Duo2.png "Duo Configuration 2")
5. Note down the Integration Key, Secret Key and API hostname. Depending on the Duo tenant, these values can also be called Client ID, Client Secret, and API hostname respectively. These will be used in the node configuration.
![alt text](https://github.com/ForgeRock/duo-auth-node/blob/master/images/Duo3.png?raw=true "Duo Configuration 3")

**ForgeRock Configuration**
1. Log into your ForgeRock AM console.
2. Create a new Authentication Tree.
![alt text](https://github.com/ForgeRock/duo-auth-node/blob/master/images/ForgeRock1.png?raw=true "ForgeRock Configuration 1")
3. Setup the following configuration for the tree that was just created.
![alt text](https://github.com/ForgeRock/duo-auth-node/blob/master/images/ForgeRock2.png?raw=true "ForgeRock Configuration 2")
4. Paste in the Integration Key (Client ID), Secret Key (Client Secret) and API hostname for the corresponding Duo Web SDK Application.
5. Generate an application key. It must be at least 40 characters long random string. You can generate a random string in Python with:
```python
import os, hashlib
print hashlib.sha1(os.urandom(32)).hexdigest()
```
6. Paste in your application key into the corresponding field in the node configuration.
7. Set Duo Javascript URL.


**Usage**
1. Log into the Tree that was created in the steps above by going to https://<url>/am/XUI/?realm=alpha&authIndexType=service&authIndexValue=Duo
2. Log in the your ForgeRock username and password.
![alt text](https://github.com/ForgeRock/duo-auth-node/blob/master/images/Access1.png?raw=true "Access 1")
3. Follow the prompts to register a new device or if you've already registered, use Duo to log in.
![alt text](https://github.com/ForgeRock/duo-auth-node/blob/master/images/Access2.png?raw=true "Access 2")

