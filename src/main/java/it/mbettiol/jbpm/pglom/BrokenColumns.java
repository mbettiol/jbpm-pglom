package it.mbettiol.jbpm.pglom;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@AllArgsConstructor
@Builder
@Data
public class BrokenColumns {

	@Singular private final List<BrokenColumn> shouldBeOidColumns;
	@Singular private final List<BrokenColumn> justUnlinkLoColumns;
	
	public static BrokenColumns forJbpm6() {
		return _forJbpm6().build();
	}
	
	public static BrokenColumns forJbpm7() {
		return _forJbpm6()
				.shouldBeOidColumn(BrokenColumn.builder().tableName("executionerrorinfo").columnName("error_info").build())
				.build();
	}
	
	private static BrokenColumns.BrokenColumnsBuilder _forJbpm6(){
		return BrokenColumns.builder()
				
				.shouldBeOidColumn(BrokenColumn.builder().tableName("booleanexpression").columnName("expression").build())
				.shouldBeOidColumn(BrokenColumn.builder().tableName("email_header").columnName("body").build())
				.shouldBeOidColumn(BrokenColumn.builder().tableName("i18ntext").columnName("text").build())
				.shouldBeOidColumn(BrokenColumn.builder().tableName("task_comment").columnName("text").build())
				.shouldBeOidColumn(BrokenColumn.builder().tableName("querydefinitionstore").columnName("qexpression").build())
				.shouldBeOidColumn(BrokenColumn.builder().tableName("deploymentstore").columnName("deploymentunit").build())
			
				.justUnlinkLoColumn(BrokenColumn.builder().tableName("content").columnName("content").build())
				.justUnlinkLoColumn(BrokenColumn.builder().tableName("processinstanceinfo").columnName("processinstancebytearray").build())
				.justUnlinkLoColumn(BrokenColumn.builder().tableName("requestinfo").columnName("requestdata").build())
				.justUnlinkLoColumn(BrokenColumn.builder().tableName("requestinfo").columnName("responsedata").build())
				.justUnlinkLoColumn(BrokenColumn.builder().tableName("sessioninfo").columnName("rulesbytearray").build())
				.justUnlinkLoColumn(BrokenColumn.builder().tableName("workiteminfo").columnName("workitembytearray").build());
	}
}
