The java-client-me-sdk sources are added to the Eclipse project using Eclipse linked folder feature.
You just need to copy java-client-me-sdk sources to the project parent folder and Eclipse should add sources to the project path automatically.

If it doesn't work or if you want to define your own path to the sources folder just change path to the sources in .project file:
You need to change locationURI variable in 'linkedResources -> link' where [$%7BPARENT-1-PROJECT_LOC%7D] is parent folder:

	<linkedResources>
		<link>
			<name>java-client-me-sdk</name>
			<type>2</type>
			<locationURI>$%7BPARENT-2-PROJECT_LOC%7D/clients-java/java-me-client/src/main/java</locationURI>
		</link>
	</linkedResources>

If you need to jump more than one folder up, just replace number 1 with other. For example:
PROJECT_LOC => C:\projects\workspace\project
PARENT-1-PROJECT_LOC => C:\projects\workspace
PARENT-2-PROJECT_LOC => C:\projects\
PARENT-3-PROJECT_LOC => C:\
