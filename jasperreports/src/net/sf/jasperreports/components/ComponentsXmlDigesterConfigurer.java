/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2023 Cloud Software Group, Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.components;

import org.apache.commons.digester.Digester;

import net.sf.jasperreports.components.iconlabel.IconLabelComponentDigester;
import net.sf.jasperreports.components.items.Item;
import net.sf.jasperreports.components.items.ItemProperty;
import net.sf.jasperreports.components.items.ItemPropertyXmlFactory;
import net.sf.jasperreports.components.items.ItemXmlFactory;
import net.sf.jasperreports.components.list.DesignListContents;
import net.sf.jasperreports.components.list.ListComponent;
import net.sf.jasperreports.components.list.StandardListComponent;
import net.sf.jasperreports.components.sort.SortComponentDigester;
import net.sf.jasperreports.components.table.DesignBaseCell;
import net.sf.jasperreports.components.table.DesignCell;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardColumnGroup;
import net.sf.jasperreports.components.table.StandardGroupCell;
import net.sf.jasperreports.components.table.StandardGroupRow;
import net.sf.jasperreports.components.table.StandardRow;
import net.sf.jasperreports.components.table.StandardTableFactory;
import net.sf.jasperreports.components.table.TableComponent;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.component.XmlDigesterConfigurer;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.PrintOrderEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.xml.DatasetRunReportContextRule;
import net.sf.jasperreports.engine.xml.JRExpressionFactory;
import net.sf.jasperreports.engine.xml.JRXmlConstants;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import net.sf.jasperreports.engine.xml.StyleContainerRule;
import net.sf.jasperreports.engine.xml.UuidPropertyRule;
import net.sf.jasperreports.engine.xml.XmlConstantPropertyRule;

/**
 * XML digester for built-in component implementations.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @see ComponentsExtensionsRegistryFactory
 */
public class ComponentsXmlDigesterConfigurer implements XmlDigesterConfigurer
{
	
	private static final String[] BARCODE4J_IGNORED_PROPERTIES = {
			JRXmlConstants.ATTRIBUTE_evaluationTime,
			"orientation",
			"textPosition"};

	private static final String[] QRCODE_IGNORED_PROPERTIES = {
			JRXmlConstants.ATTRIBUTE_evaluationTime,
			"errorCorrectionLevel"};
	
	@Override
	public void configureDigester(Digester digester)
	{
		addListRules(digester);
		addTableRules(digester);
		SortComponentDigester.addSortComponentRules(digester);
		IconLabelComponentDigester.addIconLabelComponentRules(digester);
	}

	protected void addListRules(Digester digester)
	{
		String listPattern = "*/componentElement/list";
		digester.addObjectCreate(listPattern, StandardListComponent.class);
		digester.addSetProperties(listPattern,
				//properties to be ignored by this rule
				new String[]{"printOrder"}, 
				new String[0]);
		digester.addRule(listPattern, new XmlConstantPropertyRule(
				"printOrder", "printOrderValue", PrintOrderEnum.values()));
		
		String listContentsPattern = listPattern + "/listContents";
		digester.addObjectCreate(listContentsPattern, DesignListContents.class);
		digester.addSetProperties(listContentsPattern);
		digester.addSetNext(listContentsPattern, "setContents");
		// rule to set the context dataset name
		digester.addRule(listContentsPattern, new DatasetRunReportContextRule<>(ListComponent.class));
	}
	
	protected <T> void addPatternExpressionRules(Digester digester, String barcodePattern)
	{
		String patternExpressionPattern = barcodePattern + "/patternExpression";
		digester.addFactoryCreate(patternExpressionPattern, 
				JRExpressionFactory.class.getName());
		digester.addCallMethod(patternExpressionPattern, "setText", 0);
		digester.addSetNext(patternExpressionPattern, "setPatternExpression", 
				JRExpression.class.getName());
	}
	
	protected <T> void addBarcodeRules(Digester digester, 
			String barcodePattern, Class<T> barcodeComponentClass,
			String[] ignoredProperties)
	{
		digester.addObjectCreate(barcodePattern, barcodeComponentClass);
		digester.addSetProperties(barcodePattern,
				//properties to be ignored by this rule
				ignoredProperties, 
				new String[0]);
		//rule to set evaluation time
		digester.addRule(barcodePattern, 
				new XmlConstantPropertyRule(
						JRXmlConstants.ATTRIBUTE_evaluationTime, "evaluationTimeValue",
						EvaluationTimeEnum.values()));
		
		String codeExpressionPattern = barcodePattern + "/codeExpression";
		digester.addFactoryCreate(codeExpressionPattern, 
				JRExpressionFactory.class.getName());
		digester.addCallMethod(codeExpressionPattern, "setText", 0);
		digester.addSetNext(codeExpressionPattern, "setCodeExpression", 
				JRExpression.class.getName());
	}

	protected void addTemplateRules(Digester digester, String barcodePattern)
	{
		String templateExpressionPattern = barcodePattern + "/templateExpression";
		digester.addFactoryCreate(templateExpressionPattern, 
				JRExpressionFactory.class.getName());
		digester.addCallMethod(templateExpressionPattern, "setText", 0);
		digester.addSetNext(templateExpressionPattern, "setTemplateExpression", 
				JRExpression.class.getName());
	}

	protected void addItemRules(Digester digester, String itemPattern, String methodName, String namespace)
	{
		digester.addFactoryCreate(itemPattern, ItemXmlFactory.class.getName());
		digester.addSetNext(itemPattern, methodName, Item.class.getName());

		String locationItemPropertyPattern = itemPattern + "/itemProperty";
		digester.addFactoryCreate(locationItemPropertyPattern, ItemPropertyXmlFactory.class.getName());
		digester.addSetNext(locationItemPropertyPattern, "addItemProperty", ItemProperty.class.getName());

		digester.setRuleNamespaceURI(namespace);
		
		String locationItemPropertyValueExpressionPattern = locationItemPropertyPattern + "/" + JRXmlConstants.ELEMENT_valueExpression;
		digester.addFactoryCreate(locationItemPropertyValueExpressionPattern, JRExpressionFactory.class.getName());
		digester.addCallMethod(locationItemPropertyValueExpressionPattern, "setText", 0);
		digester.addSetNext(locationItemPropertyValueExpressionPattern, "setValueExpression", JRExpression.class.getName());
	}

	
	protected void addTableRules(Digester digester)
	{
		String tablePattern = "*/componentElement/table";
		//digester.addObjectCreate(tablePattern, StandardTable.class);
		digester.addFactoryCreate(tablePattern, StandardTableFactory.class.getName());
		
		String columnPattern = "*/column";
		digester.addObjectCreate(columnPattern, StandardColumn.class);
		digester.addSetNext(columnPattern, "addColumn");
		digester.addSetProperties(columnPattern,
				//properties to be ignored by this rule
				new String[]{"uuid"}, 
				new String[0]);
		digester.addRule(columnPattern, new UuidPropertyRule("uuid", "UUID"));
		addExpressionRules(digester, columnPattern + "/printWhenExpression", 
				JRExpressionFactory.class, "setPrintWhenExpression",
				true);
		addTableCellRules(digester, columnPattern + "/tableHeader", "setTableHeader");
		addTableCellRules(digester, columnPattern + "/tableFooter", "setTableFooter");
		addTableGroupCellRules(digester, columnPattern + "/groupHeader", "addGroupHeader");
		addTableGroupCellRules(digester, columnPattern + "/groupFooter", "addGroupFooter");
		addTableCellRules(digester, columnPattern + "/columnHeader", "setColumnHeader");
		addTableCellRules(digester, columnPattern + "/columnFooter", "setColumnFooter");
		addTableCellRules(digester, columnPattern + "/detailCell", "setDetailCell");
		
		String columnGroupPattern = "*/columnGroup";
		digester.addObjectCreate(columnGroupPattern, StandardColumnGroup.class);
		digester.addSetNext(columnGroupPattern, "addColumn");
		digester.addSetProperties(columnGroupPattern,
				//properties to be ignored by this rule
				new String[]{"uuid"}, 
				new String[0]);
		digester.addRule(columnGroupPattern, new UuidPropertyRule("uuid", "UUID"));
		addExpressionRules(digester, columnGroupPattern + "/printWhenExpression", 
				JRExpressionFactory.class, "setPrintWhenExpression",
				true);
		addTableCellRules(digester, columnGroupPattern + "/tableHeader", "setTableHeader");
		addTableCellRules(digester, columnGroupPattern + "/tableFooter", "setTableFooter");
		addTableGroupCellRules(digester, columnGroupPattern + "/groupHeader", "addGroupHeader");
		addTableGroupCellRules(digester, columnGroupPattern + "/groupFooter", "addGroupFooter");
		addTableCellRules(digester, columnGroupPattern + "/columnHeader", "setColumnHeader");
		addTableCellRules(digester, columnGroupPattern + "/columnFooter", "setColumnFooter");

		addTableRowRules(digester, tablePattern + "/tableHeader", "setTableHeader");
		addTableRowRules(digester, tablePattern + "/tableFooter", "setTableFooter");
		addTableGroupRowRules(digester, tablePattern + "/groupHeader", "addGroupHeader");
		addTableGroupRowRules(digester, tablePattern + "/groupFooter", "addGroupFooter");
		addTableRowRules(digester, tablePattern + "/columnHeader", "setColumnHeader");
		addTableRowRules(digester, tablePattern + "/columnFooter", "setColumnFooter");
		addTableRowRules(digester, tablePattern + "/detail", "setDetail");
		
		addTableBaseCellRules(digester, tablePattern + "/noData", "setNoData");
	}
	
	protected void addTableBaseCellRules(Digester digester, String pattern, 
			String setNextMethod)
	{
		digester.addObjectCreate(pattern, DesignBaseCell.class);
		digester.addSetNext(pattern, setNextMethod);
		// rule to set the context dataset name
		digester.addRule(pattern, new DatasetRunReportContextRule<>(TableComponent.class));
		
		digester.addSetProperties(pattern,
				new String[]{JRXmlConstants.ATTRIBUTE_style}, 
				new String[0]);
		digester.addRule(pattern, new StyleContainerRule());
	}
	
	protected void addTableCellRules(Digester digester, String pattern, 
			String setNextMethod)
	{
		digester.addObjectCreate(pattern, DesignCell.class);
		digester.addSetNext(pattern, setNextMethod);
		// rule to set the context dataset name
		digester.addRule(pattern, new DatasetRunReportContextRule<>(TableComponent.class));
		
		digester.addSetProperties(pattern,
				new String[]{JRXmlConstants.ATTRIBUTE_style}, 
				new String[0]);
		digester.addRule(pattern, new StyleContainerRule());
	}
	
	protected void addTableGroupCellRules(Digester digester, String pattern, 
			String setNextMethod)
	{
		digester.addObjectCreate(pattern, StandardGroupCell.class);
		digester.addSetProperties(pattern);
		addTableCellRules(digester, pattern + "/cell", "setCell");
		digester.addSetNext(pattern, setNextMethod);
	}

	protected void addTableRowRules(Digester digester, String pattern, 
			String setNextMethod)
	{
		digester.addObjectCreate(pattern, StandardRow.class);
		digester.addSetProperties(pattern,
				//properties to be ignored by this rule
				new String[]{"splitType"}, 
				new String[0]);
		digester.addRule(pattern, new XmlConstantPropertyRule(
				"splitType", "splitType", SplitTypeEnum.values()));
		digester.addSetNext(pattern, setNextMethod);
		addExpressionRules(digester, pattern + "/printWhenExpression", 
				JRExpressionFactory.class, "setPrintWhenExpression",
				true);
	}
	
	protected void addTableGroupRowRules(Digester digester, String pattern, 
			String setNextMethod)
	{
		digester.addObjectCreate(pattern, StandardGroupRow.class);
		digester.addSetProperties(pattern);
		addTableRowRules(digester, pattern + "/row", "setRow");
		digester.addSetNext(pattern, setNextMethod);
	}

	protected <T> void addExpressionRules(Digester digester, String expressionPattern,
			Class<T> factoryClass, String setterMethod, boolean jrNamespace)
	{
		String originalNamespace = digester.getRuleNamespaceURI();
		if (jrNamespace)
		{
			digester.setRuleNamespaceURI(JRXmlWriter.JASPERREPORTS_NAMESPACE.getNamespaceURI());
		}
		
		digester.addFactoryCreate(expressionPattern, factoryClass);
		digester.addCallMethod(expressionPattern, "setText", 0);
		digester.addSetNext(expressionPattern, setterMethod,
				JRExpression.class.getName());
		
		if (jrNamespace)
		{
			digester.setRuleNamespaceURI(originalNamespace);
		}
	}
}
