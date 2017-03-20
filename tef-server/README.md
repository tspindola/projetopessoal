TEF-SERVER LISTO FACIL 

Esse projeto é uma customização e aperfeiçoamento do projeto jPOS-EE com as seguintes configurações:

Dependencies do projeto:

	jPOS Extended Edition
	A framework for building enterprise grade jPOS application whilst fostering code reuse.
	- https://github.com/jpos/jPOS-EE

Estrutura de arquivos

```
#!tree

	
├── build.gradle
├── contributors.txt
├── docs
│   └── jPOS-EE.pdf
└── src
    ├── dist (Configuracoes dos Perfils de execucao)
    │   ├── client
    │   │   ├── cfg (mensagens iso utilizadas pelo cliente)
    │   │   │   ├── init_exp.xml
    │   │   │   └── init_req.xml
    │   │   ├── deploy (arquivos de convfiguracao do cliente)
    │   │   │   ├── 00_logger.xml
    │   │   │   ├── 10_clientsimulator_channel.xml
    │   │   │   ├── 20_clientsimulator_mux.xml
    │   │   │   ├── 25_clientsimulator_ui.xml
    │   │   │   ├── 30_clientsimulator.xml.BAD
    │   │   │   └── 99_sysmon.xml
    │   │   └── q2.log
    │   └── server
    │       ├── cfg (scripts BeanShell para customizacao do comportamento do servidor)
    │       │   ├── context.bsh
    │       │   ├── script0800.bsh
    │       │   ├── script0800_init.bsh
    │       │   ├── script0800_logon.bsh
    │       │   └── serversimulator.bsh
    │       ├── deploy (arquivos de configuracao do servidor)
    │       │   ├── 00_logger.xml
    │       │   ├── 05_serversimulator.xml
    │       │   └── 99_sysmon.xml
    │       ├── q2.log
    │       └── resources (mensagens iso xml padroes para comunicacao com o servidor)
    │           ├── 0810_0.xml
    │           ├── 0810_0_ini.xml
    │           ├── 0810_1.xml
    │           ├── 0810_2.xml
    │           ├── 0810_3.xml
    │           ├── 0810_4.xml
    │           ├── 0810_5.xml
    │           └── 0810_50.xml
    └── main (codigo fonte java)
        └── java
            ├── br
            │   └── listofacil (codigo especifico listofacil)
            ├── com
            │   └── bravado (codigos de infra estrutura do servidor desenvolvido pela Bravado)
            ├── net
            │   └── hairi (Integracoes com o Thales HSM)
            └── org
                └── jpos (Classes especificas de testes do jPOS)


```

Arquitetura Inicial

PDV/CLIENT -> ISOXML -> TEF-SERVER -> BUSINESS RULES -> DATABASE -> ISOXML -> PDV/CLIENT

Planejamento pro futuro proximo

PDV/CLIENT -> ISOXML -> TEF-SERVER (async requests) -> BUSINESS RULES -> (JMS filas de processamento) -> SERVICOS -> TEF-SERVER -> ISOXML -> PDV/CLIENT

Gradle (http://gradle.org/) é usado para o gerenciamento de dependencias, build e execuçāo.


```
#!shellscript

$./gradlew clean build
$./gradlew runServer
$./gradlew runClient
```