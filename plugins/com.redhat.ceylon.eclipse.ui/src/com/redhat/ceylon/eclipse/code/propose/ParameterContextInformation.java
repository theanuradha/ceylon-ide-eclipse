package com.redhat.ceylon.eclipse.code.propose;

import static com.redhat.ceylon.eclipse.code.propose.CeylonContentProposer.appendDeclarationText;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Parameter;
import com.redhat.ceylon.compiler.typechecker.model.ParameterList;
import com.redhat.ceylon.compiler.typechecker.model.ProducedReference;
import com.redhat.ceylon.compiler.typechecker.model.ProducedTypedReference;
import com.redhat.ceylon.compiler.typechecker.model.Unit;
import com.redhat.ceylon.eclipse.code.outline.CeylonLabelProvider;

public class ParameterContextInformation implements IContextInformation {
	
	private Declaration declaration;
	private ProducedReference producedReference;
	private ParameterList parameterList;
	int offset;
    private Unit unit;
		
	public ParameterContextInformation(Declaration declaration,
			ProducedReference producedReference, Unit unit,
			ParameterList parameterList, int offset) {
		this.declaration = declaration;
		this.producedReference = producedReference;
        this.unit = unit;
		this.parameterList = parameterList;
		this.offset = offset;
	}

	@Override
	public String getContextDisplayString() {
		return declaration.getName();
	}
	
	@Override
	public Image getImage() {
		return CeylonLabelProvider.getImageForDeclaration(declaration);
	}
	
	@Override
	public String getInformationDisplayString() {
		return getParametersInfo(parameterList, producedReference, unit);
	}
	
	public static String getParametersInfo(ParameterList parameterList, 
			ProducedReference producedReference, Unit unit) {
		if (parameterList.getParameters().isEmpty()) {
			return "no parameters";
		}
		StringBuilder sb = new StringBuilder();
		for (Parameter p: parameterList.getParameters()) {
		    ProducedTypedReference pr = producedReference.getTypedParameter(p);
		    appendDeclarationText(p.getModel(), pr, unit, sb);
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object that) {
		if (that instanceof ParameterContextInformation) {
			return ((ParameterContextInformation) that).declaration.equals(declaration);
		}
		else {
			return false;
		}
		
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
}
