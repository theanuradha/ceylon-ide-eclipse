package com.redhat.ceylon.eclipse.code.refactor;

import static com.redhat.ceylon.eclipse.code.editor.Util.getCurrentEditor;
import static com.redhat.ceylon.eclipse.code.refactor.RenameDeclarationLinkedMode.useLinkedMode;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.texteditor.ITextEditor;

import com.redhat.ceylon.eclipse.code.editor.CeylonEditor;

public class RenameHandler extends AbstractHandler {
        
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ITextEditor editor = (ITextEditor) getCurrentEditor();
    	if (useLinkedMode() && editor instanceof CeylonEditor) {
    	    CeylonEditor ce = (CeylonEditor)editor;
            if (ce.isInLinkedMode()) {
    	        if (ce.getLinkedModeOwner() instanceof RenameDeclarationLinkedMode) {
    	            RenameDeclarationLinkedMode current = (RenameDeclarationLinkedMode) ce.getLinkedModeOwner();
                    current.enterDialogMode();
                    current.openDialog();
    	        }
    	        else {
    	            new RenameRefactoringAction(editor).run();
    	        }
    	    }
    	    else {
    	        new RenameDeclarationLinkedMode(ce).start();
    	    }
    	}
    	else {
    		new RenameRefactoringAction(editor).run();
    	}
        return null;
    }
            
}
