package it.mbettiol.jbpm.pglom;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;

import it.mbettiol.jbpm.pglom.JbpmLoTriggersScriptGenerator.UnlinkTableColumnModel.UnlinkTableColumnModelBuilder;

@RequiredArgsConstructor
public class JbpmLoTriggersScriptGenerator {
	
	private final BrokenColumns brokenColumns;
	private final String prefix;
	
	/**
	 * Generate Oid Column and Sync Trigger
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void generateCopyOidTriggers(Writer writer) throws IOException {
		List<CopyToOidModel> wrongColumns = createCopyOidModel();
		MustacheFactory mf = new DefaultMustacheFactory();;
		Mustache m = mf.compile("jbpm-set_oid.sql.mustache");
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("wrongTypeColumns", wrongColumns);
		m.execute(writer, context).flush();
	}
	
	protected List<CopyToOidModel> createCopyOidModel(){
		final String fix_column_prefix = prefix+"_oid_";
		final String fix_function_set_oid_prefix = prefix+"_oid_set";
		final String fix_column_trg_suffix = "trg";
		
		List<BrokenColumn> jbpm6WrongColumns = brokenColumns.getShouldBeOidColumns();
		List<CopyToOidModel> wrongColumns = jbpm6WrongColumns.stream()
				.map(t -> {
					String tableName = t.getTableName();
					String sourceColumnName = t.getColumnName();
					String targetColumnName = fix_column_prefix + t.getColumnName();
					String triggerName = String.join("$",fix_function_set_oid_prefix,tableName,sourceColumnName,fix_column_trg_suffix);
					String functionName = String.join("$",fix_function_set_oid_prefix,tableName,sourceColumnName);
					return CopyToOidModel.builder()
							.tableName(tableName)
							.sourceColumnName(sourceColumnName)
							.targetColumnName(targetColumnName)
							.triggerName(triggerName)
							.functionName(functionName)
							.build();
				}).collect(Collectors.toList());
		return wrongColumns;
	}
	
	
	
	/**
	 * Generate 1 unlink trigger for each table having at least 1 oid column
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void generateUnlinkLoTriggers(Writer writer) throws IOException {
		
		Map<String, UnlinkTableColumnModelBuilder> buildersByTable = 
				new LinkedHashMap<String, UnlinkTableColumnModelBuilder>() {

					@Override
					public UnlinkTableColumnModelBuilder get(Object key) {
						computeIfAbsent((String) key, s -> UnlinkTableColumnModel.builder().prefix(prefix));
						return super.get(key);
					}
			
		};
		brokenColumns.getJustUnlinkLoColumns().stream()
			.forEach(
				t -> buildersByTable.get(t.getTableName())
					.tableName(t.getTableName())
					.tableColumn(t.getColumnName())
				);
		
		
		createCopyOidModel().stream()
			.forEach(t -> buildersByTable.get(t.getTableName())
						.tableName(t.getTableName())
						.tableColumn(t.getTargetColumnName()));
		
		List<UnlinkTableColumnModel> unlinkLargeObjectTables =
				buildersByTable.values().stream().map( t-> t.build()).collect(Collectors.toList());
		

		Map<String, Object> context = new HashMap<String, Object>();
		context.put("unlinkLargeObjectTables", unlinkLargeObjectTables);
		
		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache m = mf.compile("jbpm-cleanup_lo.sql.mustache");
		m.execute(writer, context).flush();
		
	}
	
	@Builder
	@Getter
	@Setter
	@ToString
	public static class CopyToOidModel{
		
		private final String tableName;
		private final String sourceColumnName;
		private final String targetColumnName;
		private final String triggerName;
		private final String functionName;

	}
	
	@Builder
	@Getter
	@Setter
	@ToString
	public static class UnlinkTableColumnModel{
		
		private final String prefix;
		private final String tableName;
		@Singular private final List<String> tableColumns;
		
		public String getCleanupLoTriggerName() {
			return prefix+"_oid_unlink$"+tableName+"$trg";
		}
		
		public String getCleanupLoFunctionName() {
			return prefix+"_oid_unlink$"+tableName;
		}
	}
	
}
