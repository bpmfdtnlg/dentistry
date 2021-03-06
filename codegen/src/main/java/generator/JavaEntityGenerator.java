package generator;

import com.sun.codemodel.*;
import generator.total.JCodeModelUtils;
import mate.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;

public class JavaEntityGenerator extends AbstractGenerator {

	private final boolean comments;
	protected JCodeModel cm;
	public static int CONST_MODS = JMod.PUBLIC + JMod.FINAL + JMod.STATIC;

	public JavaEntityGenerator(JCodeModel cm, boolean comments) {
		this.cm = cm;
		this.comments = comments;
	}

	@Override
	public void generate(Entity type) {
		try {
			_generate(type);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	private void _generate(Entity entity) throws Exception{
		JDefinedClass entityClazz = cm._class(Project.getInstance().getEntityClassName(entity));
		entityClazz._extends(cm.ref(Project.getInstance().getBasicEntityClassName()));
		entityClazz.javadoc().add(entity.getDescription());
		entityClazz.field(JMod.PRIVATE + JMod.FINAL + JMod.STATIC, cm.LONG, "serialVersionUID", JExpr.lit(1L));

		List<Property> joinProps = new ArrayList<Property>();
		for(Property property : entity.getProperties()){
			if(property.getJoinEntity() == null && property.getJoins().isEmpty())
				continue;
			joinProps.add(property);
		}

		// entity
		ConstGroup[] constGroups = entity.getConstGroups();
		if(!ArrayUtils.isEmpty(constGroups)){
			for(ConstGroup cg : constGroups){
				generateConsts(entityClazz, comments, cg);
			}
		}
		generateProperties(entityClazz, comments, entity.getProperties());

		// vo
		JDefinedClass voClazz = cm._class(Project.getInstance().getValueObjectClassName(entity));
		voClazz._extends(entityClazz);
		voClazz.field(JMod.PRIVATE + JMod.FINAL + JMod.STATIC, cm.LONG, "serialVersionUID", JExpr.lit(1L));
		for (Property property : joinProps) {
			Entity joinEntity = property.getJoinEntity();
			if (joinEntity != null) {
				String propertyName = StringUtils.uncapitalize(property.getName().substring(0, property.getName().length() - 2));
				Property prop = new Property(joinEntity, propertyName, "");

				if (property.getJoinEntity().isEntityLike() || "LoginInfo".equals(property.getJoinEntity().getName())) {
					prop = new Property("java.util.Map<String, Object> ", propertyName, "");
				}

				generateProperties(voClazz, false, prop);
			}
			for (Join join : property.getJoins()) {
				generateJoinVo(voClazz, join);
			}
		}
		for (Join join : entity.getJoins()) {
			generateJoinVo(voClazz, join);
		}

		// qo
		JDefinedClass qoClazz = cm._class(Project.getInstance().getQueryConditionClassName(entity));
		qoClazz._extends(cm.directClass(Project.getInstance().getQueryConditionClassName()));

		List<Property> properties = new LinkedList<Property>();
		properties.add(new Property("Long[]", "ids", "ID ??????"));
		properties.add(new Property("java.util.Date", "startCreatedDate", "???????????????????????? - ??????"));
		properties.add(new Property("java.util.Date", "endCreatedDate", "???????????????????????? - ??????"));
		qoClazz.method(JMod.PUBLIC, cm.BOOLEAN, "hasIds").body()._return(JExpr.refthis("ids").ref("length").ne(JExpr.lit(0)));

		for (Property property : joinProps) {
			if (property.getJoinEntity() != null) {
				String substringName = property.getName().substring(0, property.getName().length() - 2);
				JCodeModelUtils.generateProperty(qoClazz, cm.ref(Project.getInstance().getJoinTypeClassName()), "join" + StringUtils.capitalize(substringName));

				if (!property.getJoinEntity().isEntityLike() && !"LoginInfo".equals(property.getJoinEntity().getName())) {
					JClass joinQoType = cm.ref(Project.getInstance().getQueryConditionClassName(property.getJoinEntity()));
					String joinQoName = substringName + "Query";
					JCodeModelUtils.generateProperty(qoClazz, joinQoType, joinQoName);
				}

				String setJoinMethodName = "setJoin" + StringUtils.capitalize(substringName);
				JMethod setMethod = qoClazz.method(JMod.PUBLIC, qoClazz, setJoinMethodName);
				JBlock body = setMethod.body();
				body.invoke(JExpr._this(), setJoinMethodName).arg(cm.ref(Project.getInstance().getJoinTypeClassName()).staticRef("LEFT"));
				body._return(JExpr._this());
			}

			for (Join join : property.getJoins()) {
				generateJoinQuery(qoClazz, join);
			}
		}
		for (Join join : entity.getJoins()) {
			generateJoinQuery(qoClazz, join);
		}

		Entity.MappingSearch mappingSearch = entity.getMappingSearch();
		if (mappingSearch != null) {
			properties.add(new Property("Long", mappingSearch.getSearchPropertyName(), "??????"));
		}

		for(Property property : entity.getProperties()){

			String type = property.getType();

			if(property.isSearchable())
				properties.add(new Property(type, property.getName(), property.getDescription()));

			if(property.isLarge() && property.getType().equals("String"))
				properties.add(new Property("Boolean", "select" + StringUtils.capitalize(property.getName()), "????????????"));

			if(property.isNullSearchale())
				properties.add(new Property("Boolean", "null" + StringUtils.capitalize(property.getName()), "????????? NULL ???"));

			if(property.isNotEqualSearchable()) {
				properties.add(new Property(type, property.getName() + "NotEqualTo", "?????????"));
			}

			if(property.isNullOrEqualTo()) {
				properties.add(new Property(type, property.getName() + "IsNullOrEqualTo", "?????? NULL ?????????"));
			}

			if(!property.isLikeSearchable())
				continue;

			String capitalizedName = StringUtils.capitalize(property.getName());
			if(type.equals(BigDecimal.class.getName()) || type.equals(Integer.class.getSimpleName())
					|| type.equals(Short.class.getSimpleName())
					|| type.equals(Byte.class.getSimpleName())){
				properties.add(new Property(type, "min" + capitalizedName, property.getDescription() + " - ??????"));
				properties.add(new Property(type, "max" + capitalizedName, property.getDescription() + " - ??????"));
			}else if(type.equals(Date.class.getName())){
				properties.add(new Property(type, "start" + capitalizedName, property.getDescription() + " - ??????"));
				properties.add(new Property(type, "end" + capitalizedName, property.getDescription() + " - ??????"));

				JMethod m = qoClazz.method(JMod.PUBLIC, qoClazz, "set" + capitalizedName);
				m.param(Date.class, "start" + capitalizedName);
				m.param(Date.class, "end" + capitalizedName);
				JBlock body = m.body();
				body.invoke("setStart" + capitalizedName).arg(JExpr.ref("start" + capitalizedName));
				body.invoke("setEnd" + capitalizedName).arg(JExpr.ref("end" + capitalizedName));
				body._return(JExpr._this());

			}else if(type.equals("String")){
				properties.add(new Property(type, "like" + capitalizedName, property.getDescription()));
			}else if(type.equals("Long")){
				properties.add(new Property("Long[]", property.getName() + "s", property.getDescription()));
			}
		}
		if(!ArrayUtils.isEmpty(constGroups)){
			for(ConstGroup cg : constGroups){
				String type = (cg.getMethod() == ConstGroup.METHOD_MOD) ? "Long[]" : "Integer[]";
				String suffix = (cg.getMethod() == ConstGroup.METHOD_MOD) ? "" : "s";
				Property property = null;
				if(cg.isInclude()){
					property = new Property(type, "include" + StringUtils.capitalize(cg.getName()) + suffix, "");
					properties.add(property);
				}
				if(cg.isExclude()){
					property = new Property(type, "exclude" + StringUtils.capitalize(cg.getName()) + suffix, "");
					properties.add(property);
				}
			}
		}
		Property[] propertiesArray = new Property[properties.size()];
		generateProperties(qoClazz, false, properties.toArray(propertiesArray));

		generateOrderByMethod(qoClazz, "id");
		generateOrderByMethod(qoClazz, "createdDate");
		for(Property property : entity.getProperties()){
			if(!property.isSortable())
				continue;
			generateOrderByMethod(qoClazz, property.getName());
		}

		if (entity.isRandomable()) {
			JMethod orderByMethod = qoClazz.method(JMod.PUBLIC, cm.VOID, "setOrderByRandom");
			orderByMethod.param(cm.INT, "keyword");
			orderByMethod.body().invoke("setOrderBy").arg(JExpr.lit("RAND()")).arg(JExpr.ref("keyword"));
		}
	}

	private void generateJoinQuery(JDefinedClass qoClazz, Join join) {
		String name = join.getPropertyName();
		JCodeModelUtils.generateProperty(qoClazz, cm.ref(Project.getInstance().getJoinTypeClassName()), "join" + StringUtils.capitalize(name));

		if (!join.getEntity().isEntityLike() && !"LoginInfo".equals(join.getEntity().getName())) {
			JClass joinQoType = cm.ref(Project.getInstance().getQueryConditionClassName(join.getEntity()));
			String joinQoName = name + "Query";
			JCodeModelUtils.generateProperty(qoClazz, joinQoType, joinQoName);
		}

		String setJoinMethodName = "setJoin" + StringUtils.capitalize(name);
		JMethod setMethod = qoClazz.method(JMod.PUBLIC, qoClazz, setJoinMethodName);
		JBlock body = setMethod.body();
		body.invoke(JExpr._this(), setJoinMethodName).arg(cm.ref(Project.getInstance().getJoinTypeClassName()).staticRef("LEFT"));
		body._return(JExpr._this());
	}

	private void generateJoinVo(JDefinedClass voClazz, Join join) throws Exception {
		String propertyName = join.getPropertyName();
		Property prop = new Property(join.getEntity(), propertyName, "");
		if (join.getEntity().isEntityLike() || "LoginInfo".equals(join.getEntity().getName())) {
			prop = new Property("java.util.Map<String, Object> ", propertyName, "");
		}
		generateProperties(voClazz, false, prop);
	}

	private void generateOrderByMethod(JDefinedClass clazz, String propertyName) throws Exception{
		JMethod orderByMethod = clazz.method(JMod.PUBLIC, cm.VOID, "setOrderBy" + StringUtils.capitalize(propertyName));
		orderByMethod.param(cm.INT, "keyword");
		orderByMethod.body().invoke("setOrderBy").arg(JExpr.lit(propertyName)).arg(JExpr.ref("keyword"));

		orderByMethod = clazz.method(JMod.PUBLIC, cm.INT, "getOrderBy" + StringUtils.capitalize(propertyName));
		orderByMethod.body()._return(JExpr.invoke("getOrderByKeyword").arg(JExpr.lit(propertyName)));
	}

	private void generateProperties(JDefinedClass clazz, boolean withComment, Property... properties) throws Exception {
		if(ArrayUtils.isEmpty(properties))
			return;

		for (Property property : properties) {
			JType propertyType = cm.ref(property.getType());
			String propertyName = property.getName();

			JFieldVar field = clazz.field(JMod.PROTECTED, propertyType, propertyName);
			if(withComment) {
				field.javadoc().add(property.getDescription());
			}
			if(property.isGetter()) generateGetter(clazz, withComment, property);
			if(property.isSetter()) generateSetter(clazz, withComment, property);
		}
	}

	private void generateGetter(JDefinedClass clazz, boolean withComment, Property property) throws ClassNotFoundException{
		String propertyName = property.getName();
		String type = property.getType();

		String methodName = "get" + StringUtils.capitalize(propertyName);
		JMethod getterMethod = clazz.method(JMod.PUBLIC, cm.ref(type),  methodName);
		if(withComment){
			JDocComment javadoc = getterMethod.javadoc();
			javadoc.add("??????" + property.getDescription());
			javadoc.addReturn().add(property.getDescription());
		}
		JBlock body = getterMethod.body();
		body._return(JExpr.refthis(propertyName));
	}

	private void generateSetter(JDefinedClass clazz, boolean withComment, Property property) throws ClassNotFoundException{
		String propertyName = property.getName();
		String type = property.getType();

		String capitalizedPropertyName = StringUtils.capitalize(propertyName);
		String methodName = "set" + capitalizedPropertyName;
		JMethod setterMethod = clazz.method(JMod.PUBLIC, cm.VOID,  methodName);
		setterMethod.param(cm.ref(type), propertyName);
		JBlock body = setterMethod.body();
		body.assign(JExpr.refthis(propertyName), JExpr.ref(propertyName));

		if(withComment){
			JDocComment javadoc = setterMethod.javadoc();
			javadoc.add("??????" + property.getDescription());
			javadoc.addParam(propertyName).add(property.getDescription());
		}

		if(property.isAddOperation()){
			JMethod addMethod = clazz.method(JMod.PUBLIC, clazz, "add" + capitalizedPropertyName);
			if(type.equals(BigDecimal.class.getName())){
				addMethod.param(BigDecimal.class, "value");
				addMethod.body().assign(JExpr.refthis(propertyName),
						JExpr.invoke(JExpr.refthis(propertyName), "add").arg(JExpr.ref("value")))
						._return(JExpr._this());
			}else if(type.equals("Integer") || type.equals("int")){
				addMethod.param(cm.INT, "value");
				addMethod.body().assign(JExpr.refthis(propertyName),
						JExpr.refthis(propertyName).plus(JExpr.ref("value")))
						._return(JExpr._this());
			}else{
				System.err.println("???????????? add ???????????????" + property.getName());
			}
		}

		if(property.isSubtractOperation()){
			JMethod addMethod = clazz.method(JMod.PUBLIC, clazz, "subtract" + capitalizedPropertyName);
			if(type.equals(BigDecimal.class.getName())){
				addMethod.param(BigDecimal.class, "value");
				addMethod.body().assign(JExpr.refthis(propertyName),
						JExpr.invoke(JExpr.refthis(propertyName), "subtract").arg(JExpr.ref("value")))
						._return(JExpr._this());
			}else if(type.equals("Integer") || type.equals("int")){
				addMethod.param(cm.INT, "value");
				addMethod.body().assign(JExpr.refthis(propertyName),
						JExpr.refthis(propertyName).minus(JExpr.ref("value")))
						._return(JExpr.ref("this"));
			}else{
				System.err.println("???????????? subtract ???????????????" + type + "(" + propertyName + ")");
			}
		}
	}

	private void generateConsts(JDefinedClass clazz, boolean withComment, ConstGroup cg) throws Exception {
		if(cg == null)
			return;

		if(cg.getConsts() == null)
			cg.setConsts(new Const[0]);

		Const[] consts = cg.getConsts();

		// fields
		String cgType;
		if(cg.getMethod() == ConstGroup.METHOD_MOD){
			cgType = Long.class.getSimpleName();
			JFieldVar field = clazz.field(JMod.PROTECTED, cm.parseType(cgType), cg.getName(), JExpr.lit(0L));
			if (withComment) {
				JDocComment javadoc = field.javadoc();
				javadoc.add(cg.getDescription());
			}
		} else {
			cgType = Integer.class.getSimpleName();
			JFieldVar field = clazz.field(JMod.PROTECTED, cm.parseType(cgType), cg.getName());
			if (withComment) {
				JDocComment javadoc = field.javadoc();
				javadoc.add(cg.getDescription());
			}
		}

		// getter/setter
		String propertyName = cg.getName();
		{
			String capitalizedPropertyName = StringUtils.capitalize(propertyName);
			String methodName = "set" + capitalizedPropertyName;
			JMethod setterMethod = clazz.method(JMod.PUBLIC, clazz,  methodName);
			setterMethod.param(cm.ref(cgType), propertyName);
			JBlock body = setterMethod.body();
			if(cg.getMethod() == ConstGroup.METHOD_MOD){
				body._if(JExpr.ref(propertyName).eq(JExpr._null()))._then().assign(JExpr.ref(propertyName), JExpr.lit(0L));
			}
			body.assign(JExpr.refthis(propertyName), JExpr.ref(propertyName));
			body._return(JExpr._this());

			if(withComment){
				JDocComment javadoc = setterMethod.javadoc();
				javadoc.add("??????" + cg.getDescription());
				javadoc.addParam(propertyName).add(cg.getDescription());
			}
		}
		{
			String methodName = "get" + StringUtils.capitalize(propertyName);
			JMethod getterMethod = clazz.method(JMod.PUBLIC, cm.ref(cgType),  methodName);
			JBlock body = getterMethod.body();
			body._return(JExpr.refthis(propertyName));

			if(withComment){
				JDocComment javadoc = getterMethod.javadoc();
				javadoc.add("??????" + cg.getDescription());
				javadoc.addReturn().add(cg.getDescription());
			}
		}

		// valid
		if(cg.getMethod() == ConstGroup.METHOD_TYPE && cg.isValidSetter()){
			String validName = "valid" + StringUtils.capitalize(cg.getName()) + "s";

			JMethod validTypesMethod = clazz.method(JMod.PUBLIC + JMod.FINAL + JMod.STATIC, cm.parseType("Integer[]"), "get" + StringUtils.capitalize(validName));
			JBlock validTypesMethodBody = validTypesMethod.body();

			if(withComment){
				JDocComment javadoc = validTypesMethod.javadoc();
				javadoc.add("?????????????????????" + cg.getDescription());
				javadoc.addReturn().add("???????????????" + cg.getDescription());
			}

			JArray validTypesArray = JExpr.newArray(cm.parseType("Integer"));
			for (Const aConst : consts) {
				validTypesArray.add(JExpr.ref(StringUtils.upperCase(cg.getName().replaceAll("([A-Z])", "_$1")) + "_" + aConst.getName()));
			}
			validTypesMethodBody.decl(cm.parseType("Integer[]"), validName, validTypesArray);
			validTypesMethodBody._return(JExpr.ref(validName));
		}

		// consts
		String prev = null;
		String cgPrimaryType = (cg.getMethod() == ConstGroup.METHOD_MOD)  ? "long" : "int";
		JClass modClass = cm.ref(Project.getInstance().getModClassName());
		for (Const cont : consts) {
			JFieldVar field = clazz.field(CONST_MODS, cm.ref(cgPrimaryType),
					StringUtils.upperCase(cg.getName().replaceAll("([A-Z])", "_$1")) + "_" + cont.getName());

			if (withComment) {
				JDocComment javadoc = field.javadoc();
				javadoc.add(cg.getDescription() + "??????" + cont.getDescription()  + "???");
			}

			if (cg.getMethod() == ConstGroup.METHOD_MOD) {
				field.init(prev == null
						? JExpr.lit(1L)
						: JExpr.ref(prev).shl(JExpr.lit(1L)));

				JMethod hasMethod = clazz.method(JMod.PUBLIC, cm.BOOLEAN, "has" + StringUtils.capitalize(cont.getAlias()));
				hasMethod.body()._return(modClass.staticInvoke("hasMod")
						.arg(JExpr.ref(cg.getName())).arg(field));

				if(withComment){
					JDocComment javadoc = hasMethod.javadoc();
					javadoc.add("?????????" + cg.getDescription() + "??????" + cont.getDescription() + "???");
					javadoc.addReturn().add("????????? true??????????????? false???");
				}

				JMethod setMethod = clazz.method(JMod.PUBLIC, clazz, "set" + StringUtils.capitalize(cont.getAlias()) + (cgPrimaryType.equals("long") ? "" : StringUtils.capitalize(cg.getName())));
				setMethod.param(cm.BOOLEAN, "b");
				setMethod.body().assign(JExpr.refthis(cg.getName()), modClass.staticInvoke("setMod").arg(JExpr.ref(cg.getName())).arg(field).arg(JExpr.ref("b")))._return(JExpr._this());

				if(withComment){
					JDocComment javadoc = setMethod.javadoc();
					javadoc.add("???????????????" + cg.getDescription() + "??????" + cont.getDescription() + "???");
					javadoc.addParam("b").add("????????? true??????????????? false???");
				}

			} else if (cg.getMethod() == ConstGroup.METHOD_TYPE) {
				field.init(prev == null
						? JExpr.lit(0)
						: JExpr.ref(prev).plus(JExpr.lit(1)));

				JMethod isMethod = clazz.method(JMod.PUBLIC, cm.ref(Boolean.class), "is" + StringUtils.capitalize(cont.getAlias()) + StringUtils.capitalize(cg.getName()));
				JBlock methodBody = isMethod.body();
				methodBody._if(JExpr.refthis(cg.getName()).eq(JExpr._null()))._then()._return(JExpr._null());
				methodBody._return(JExpr.refthis(cg.getName()).eq(field));
				if(withComment){
					JDocComment javadoc = isMethod.javadoc();
					javadoc.add(cg.getDescription() + "???????????????" + cont.getDescription() + "???");
					javadoc.addReturn().add("????????? true??????????????? false???");
				}

				JMethod setMethod = clazz.method(JMod.PUBLIC, clazz, "set" + StringUtils.capitalize(cont.getAlias()) + StringUtils.capitalize(cg.getName()));
				setMethod.body().assign(JExpr.refthis(cg.getName()), field)._return(JExpr._this());

				if(withComment){
					JDocComment javadoc = setMethod.javadoc();
					javadoc.add("??????" + cg.getDescription() + "?????????" + cont.getDescription() + "???");
				}
			}
			prev = field.name();
		}
	}

	@Override
	public String getName() {
		return "Entity Class";
	}


}
