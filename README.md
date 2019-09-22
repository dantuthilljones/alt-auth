# alt-auth

alt-auth is a basic service designed to enable password protection on your nginx web service by acting as the endpoint of
nginx auth request module.

The nginx auth request module implements client authorization based on the result of a subrequest. If the subrequest
returns a 2xx response code, the access is allowed. If it returns 401 or 403, the access is denied with the
corresponding error code. Any other response code returned by the subrequest is considered an error.
[Read more here](https://nginx.org/en/docs/http/ngx_http_auth_request_module.html).

alt-auth uses a signed cookie as an authentication token. A new authentication cookie is set when an incoming POST
request has a `X-IS-LOGIN` header set to `true` and the `password` parameter matches the password. The password and auth
token duration are configurable.

This project also includes a static web page which can be used as the log in form.

## Installation

### Prerequisites

* This webservice written in Java and requires at least **JRE 8**
* The auth request module is was introduced in version **nginx 1.5.4**

### Installing The Service

I suggest running this as a systemd based linux service because I don't know how to use any other service manager. This
also means I only provide instructions for systemd based linux systems.

1. Create a directory for the executable and config file to live

    `sudo mkdir /usr/local/alt-auth`

2. Copy the `alt-auth.jar` file in to your folder

    `sudo cp alt-auth.jar /usr/local/alt-auth/alt-auth.jar`

3. Create your config file with your preferred text editor (See the [configuration](#Configuration File) section for
details).

    `sudo vi /usr/local/alt-auth/config.yml`
    
4. Create the .service file

    `sudo vi /lib/systemd/system/alt-auth.service`
    
    With the content
    
    ```
    [Unit]
    Description=alt-auth service
    
    [Service]
    Type=simple
    ExecStart=/usr/bin/java -jar /usr/local/alt-auth/alt-auth.jar server /usr/local/alt-auth/config.yml
    
    [Install]
    WantedBy=multi-user.target
    ```
   
5. Start the service

    `sudo systemctl daemon-reload`
    
    `sudo systemctl start alt-auth`
    
### Configuring nginx

There are several configuration mechanisms which we need for alt-auth to function correctly.

#### Pass Login Messages To Alt-Auth

alt-auth will set an authentication cookie if a request has the `X-IS-LOGIN` header set to `true`. To use this, we need
an endpoint for the login page to send the passwords to. Use this config to configure the `/login` endpoint to pass
requests to alt-auth (on port 8765) and set the `X-IS-LOGIN` header appropriately.

```
location = /login {
    proxy_pass http://localhost:8765; # Reverse proxy to alt-auth
    proxy_set_header X-IS-LOGIN true; # Tell alt-auth that this is a login request
}
```

#### Configure An Internal Authentication Endpoint

An internal endpoint is required to send the authentication subrequests through.

```
location = /auth {
    internal; # Flag this endpoint as internal only
    proxy_pass http://localhost:8765; # Reverse proxy to alt-auth
    proxy_set_header Content-Length ""; # Remove the content from the request
    proxy_set_header X-Original-URI $request_uri; # Tell alt-auth what the requested url was, not actually needed
}
```

#### Configure Sensitive Endpoints To Authenticate Via Alt-Auth

Adding this config line to any endpoint will cause nginx to send an authentication subrequest to the specified location.

```
auth_request /auth;
```

For example, this will protect `/private`

```
location /private {
    auth_request /auth;
}
```

#### Configure Sensitive Endpoints To Redirect To Login Page

Instead of displaying a 401 or 403 error page, we can redirect the user to a login page. To do this, we first need a
redirection endpoint. These begin with `@`. We will configure it to redirect to a login page at
`/login.html`.

```
location @redirectlogin {
    return 302 /login.html;
}
```

Next we need to configure our server to respond with this redirection endpoint when the authentication fails. The
following lines of config will tell nginx to respond with our redirection endpoint when nginx would normally respond
with a HTTP code of 401 or 403. To keep it simple, you can place these on the server level of your config however this
will cause all 401 and 403 responses from all endpoints to redirect with your login page. If this causes issues with
other parts of your site you can place this config on each of your sensitive endpoints.

```
error_page 401 = @redirectlogin;
error_page 403 = @redirectlogin;
```

### Installing Login Page

I provide a basic login page which can be found in the `html` directory. It is configured to send a password entered by
a user to `/login`. If the password is correct then it will redirect to your website's home page. Both the redirection
and login locations are configurable by editing the following lines in `login.html`.

```
var loginEndpoint = "/login";
var successfulLoginRedirect = "/";
```

Finally copy the contents of the `html` folder to your website's root directory so it can be served to your users.

### Configuration File

alt-auth requires a YAML config file with three fields: `port`, `password` and `duration`

#### Password

The `password` field is a scrypt hash of the password.
This project comes with a handy tool for hashing your passwords.
You can run this tool by running the .jar file with the `hash` parameter followed by your password.
e.g. 

```
java -jar authserver.jar hash password
```

produces the output

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
$s0$e1001$98QjB5j7DQjgG1/mFQm0Sg==$vwd7GDkXYvtEP/+l2tpM5zriancNurACHTGjKYdTYhI=
```

Please ignore the garbage lines starting with "SLF4J" and instead draw your attention to the last line which appears to 
made up of garbage characters. This is your hashed password.

#### Duration

The `duration` field is a duration in [ISO-8601](https://en.wikipedia.org/wiki/ISO_8601) format. This is parsed with
Java's
[Duration.parse(CharSequence text)](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-)
method.

#### Port

The `port` field is the port number for the service to run on. For no particular reason, I use `8765`.

#### Example

The following config file configures this service to run on port `8765`, authenticate against the password `password`
and create auth tokens which are valid for 30 days.

```
port: 8765
password: $s0$e1001$98QjB5j7DQjgG1/mFQm0Sg==$vwd7GDkXYvtEP/+l2tpM5zriancNurACHTGjKYdTYhI=
duration: P30D
```

## To Do List

* Use an alternative security mechanism for the authentication cookies. Currently alt-auth will RSA sign the cookies
however we but we could use a method with less overhead.

## Built With

* [Maven](https://maven.apache.org/)

* [Java](https://jdk.java.net/)

* [IntelliJ IDEA](https://www.jetbrains.com/idea/)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details