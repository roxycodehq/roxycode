# roxycode
RoxyCode is a cross platform AI coding assistant

## Usage

### Running with Maven
To run the jsmashy CLI from source using Maven, use the following command from the project root:

```bash
mvn exec:java -pl jsmashy-cli -Dexec.mainClass="org.roxycode.jsmashy.cli.Main" -Dexec.args="<input-dir> <output-file>"
```

### Convenience Script
A convenience script `jsmashy.sh` is provided in the root directory.

```bash
./jsmashy.sh <input-dir> <output-file>
```
