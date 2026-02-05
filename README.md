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

# Duo Authentication Nodes for Ping Identity

This README describes the **Duo Universal Prompt node** (current and recommended) and the **Duo node (deprecated)** for integrating Duo two-factor authentication into Ping Identity authentication journeys.

---

## Duo Universal Prompt Node (Recommended)

The **Duo Universal Prompt node** integrates with the Duo service to provide two-factor authentication using Duo’s **Universal Prompt** interface. This node uses the **Duo Web v4 SDK** and replaces the deprecated Duo node that relied on Duo’s Traditional Prompt.

The node redirects users to the Duo service, where they complete registration or authentication, and then returns them to the Ping Identity journey.

---

## Example Journey

In a typical authentication journey:

1. The user reaches the Duo Universal Prompt node.
2. The node redirects the user to the Duo service.
3. The user completes Duo registration or authentication.
4. Duo redirects the user back to Ping Identity.
5. The journey resumes based on the authentication result.

---

## Availability

| Product | Available |
|--------|-----------|
| PingOne Advanced Identity Cloud | Yes |
| PingAM (self-managed) | Yes |
| Ping Identity Platform (self-managed) | Yes |

---

## Inputs

The Duo Universal Prompt node requires the following inbound data:

| Description | Attribute Name | Source |
|------------|----------------|--------|
| Username | `username` | Shared State |

---

## Dependencies

Before using this node:

- You must have a Duo account.
- PingOne Advanced Identity Cloud must be integrated with the Duo authentication service.
- This node replaces the deprecated Duo node that used Duo’s Traditional Prompt.

---

## Configuration

The following properties are configurable:

| Property | Description |
|--------|-------------|
| Client ID | The client ID provided by Duo |
| Client Secret | The client secret provided by Duo |
| API Hostname | The Duo API hostname |
| Failure Mode When Duo Is Down | Determines behavior when Duo health checks fail:<br>• **OPEN** – Skip two-factor authentication and proceed to the **True** outcome<br>• **CLOSED** – Fail authentication and proceed to the **False** outcome |
| OpenAM Callback URL | Callback URL used to return to Ping Identity and resume the journey |

---

## Outputs

- If an **Error** outcome occurs, the error message is written to the shared state.

---

## Outcomes

| Outcome | Description |
|--------|-------------|
| True | Duo authentication succeeded |
| False | Duo authentication failed |
| Error | An error occurred during authentication |

---

## Duo Node (Deprecated)

> **Deprecated**  
> Ping Identity has deprecated the Duo node because Duo has deprecated the **Traditional Duo Prompt**.  
> Use the **Duo Universal Prompt node** instead.


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

