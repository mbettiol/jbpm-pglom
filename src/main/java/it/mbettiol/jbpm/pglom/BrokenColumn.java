package it.mbettiol.jbpm.pglom;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BrokenColumn {

	private final String tableName;
	private final String columnName;
	
}
