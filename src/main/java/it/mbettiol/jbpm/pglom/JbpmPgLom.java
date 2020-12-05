package it.mbettiol.jbpm.pglom;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public class JbpmPgLom {

	public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new JbpmPgLom.Execution()).execute(args);
        System.exit(exitCode);
	}
	
	@Command(name = "jbpm-pglom", 
			mixinStandardHelpOptions = true, version = Version.VERSION,
	        description = "jbpm pg large_object maintenance"
	        )
	static class Execution implements Callable<Integer> {


	    @Option(names = {"-p", "--prefix"}, 
	    		description = "Prefix to use in generated triggers, function and column names" ,
	    		defaultValue = "pglom",
	    		required = true)
	    private String prefix = "acme";
	    
	    @Option(names = {"-j", "--jbpm-version"}, 
	    		description = "The jbpm version ${COMPLETION-CANDIDATES}",
	    		required = true)
	    private JbpmVersion jbpmVersion;
	    
	    public enum JbpmVersion{
	    	JBPM6,
	    	JBPM7
	    }

	    @Override
	    public Integer call() throws Exception {
	    	
	    	BrokenColumns selectedJbpmVersion = selectedJbpmVersion();
	    	JbpmLoTriggersScriptGenerator jbpm6ScriptGenerator = 
	    			new JbpmLoTriggersScriptGenerator(selectedJbpmVersion,prefix);
	    	
	    	StringWriter writer = new StringWriter();
	    	jbpm6ScriptGenerator.generateCopyOidTriggers(writer);
	    	jbpm6ScriptGenerator.generateUnlinkLoTriggers(writer);
			System.out.println(writer.toString());
	    	return 0;
	    }
	    
	    protected BrokenColumns selectedJbpmVersion() {
	    	switch(jbpmVersion) {
	    	case JBPM6: return BrokenColumns.forJbpm6();
	    	case JBPM7: return BrokenColumns.forJbpm7();
	    	default: throw new IllegalArgumentException(String.format("unknown %s", jbpmVersion));
    	}
	    }
	}
}
