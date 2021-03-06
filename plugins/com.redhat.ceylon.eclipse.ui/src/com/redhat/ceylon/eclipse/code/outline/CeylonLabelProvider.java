package com.redhat.ceylon.eclipse.code.outline;

import static com.redhat.ceylon.compiler.typechecker.model.Util.isTypeUnknown;
import static com.redhat.ceylon.compiler.typechecker.tree.Util.formatPath;
import static com.redhat.ceylon.compiler.typechecker.tree.Util.hasAnnotation;
import static com.redhat.ceylon.eclipse.code.outline.CeylonLabelDecorator.getDecorationAttributes;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.ANNOTATIONS;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.IDENTIFIERS;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.KEYWORDS;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.PACKAGES;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.STRINGS;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.TYPES;
import static com.redhat.ceylon.eclipse.code.parse.CeylonTokenColorer.color;
import static org.eclipse.jface.viewers.StyledString.COUNTER_STYLER;
import static org.eclipse.jface.viewers.StyledString.QUALIFIER_STYLER;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.redhat.ceylon.compiler.typechecker.model.Class;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Interface;
import com.redhat.ceylon.compiler.typechecker.model.Method;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.model.NothingType;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.TypeAlias;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.Unit;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ModuleDescriptor;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.PackageDescriptor;
import com.redhat.ceylon.eclipse.code.search.CeylonElement;
import com.redhat.ceylon.eclipse.ui.CeylonPlugin;
import com.redhat.ceylon.eclipse.ui.CeylonResources;

/**
 * Styled Label Provider which can be used to provide labels for Ceylon elements.
 * 
 * Extends StyledCellLabelProvider to provide custom styling by doing its own painting 
 * - here the {@link #update(ViewerCell)} method is the entry point
 * Implements DelegatingStyledCellLabelProvider.IStyledLabelProvider too, but this 
 * probably is not required.
 * 
 * @author max
 *
 */
public class CeylonLabelProvider extends StyledCellLabelProvider 
        implements DelegatingStyledCellLabelProvider.IStyledLabelProvider, 
                   ILabelProvider, CeylonResources {
    
    private Set<ILabelProviderListener> fListeners = new HashSet<ILabelProviderListener>();
    
    public static ImageRegistry imageRegistry = CeylonPlugin.getInstance()
            .getImageRegistry();
    
    public static Image FILE = imageRegistry.get(CEYLON_FILE);
    public static Image FOLDER = imageRegistry.get(CEYLON_FOLDER);
    public static Image ALIAS = imageRegistry.get(CEYLON_ALIAS);
    public static Image CLASS = imageRegistry.get(CEYLON_CLASS);
    public static Image INTERFACE = imageRegistry.get(CEYLON_INTERFACE);
    public static Image LOCAL_CLASS = imageRegistry.get(CEYLON_LOCAL_CLASS);
    public static Image LOCAL_INTERFACE = imageRegistry.get(CEYLON_LOCAL_INTERFACE);
    public static Image METHOD = imageRegistry.get(CEYLON_METHOD);
    public static Image ATTRIBUTE = imageRegistry.get(CEYLON_ATTRIBUTE);
    public static Image LOCAL_METHOD = imageRegistry.get(CEYLON_LOCAL_METHOD);
    public static Image LOCAL_ATTRIBUTE = imageRegistry.get(CEYLON_LOCAL_ATTRIBUTE);
    public static Image PARAMETER = imageRegistry.get(CEYLON_PARAMETER);
    public static Image PARAMETER_METHOD = imageRegistry.get(CEYLON_PARAMETER_METHOD);
    public static Image PACKAGE = imageRegistry.get(CEYLON_PACKAGE);
    public static Image ARCHIVE = imageRegistry.get(CEYLON_ARCHIVE);
    public static Image VERSION = imageRegistry.get(MODULE_VERSION);
    public static Image IMPORT = imageRegistry.get(CEYLON_IMPORT);
    public static Image IMPORT_LIST = imageRegistry.get(CEYLON_IMPORT_LIST);
    public static Image PROJECT = imageRegistry.get(CEYLON_PROJECT);
    public static Image CORRECTION = imageRegistry.get(CEYLON_CORRECTION);
    public static Image CHANGE = imageRegistry.get(CEYLON_CHANGE);
    public static Image COMPOSITE_CHANGE = imageRegistry.get(CEYLON_COMPOSITE_CHANGE);
    public static Image RENAME = imageRegistry.get(CEYLON_RENAME);
    public static Image REORDER = imageRegistry.get(CEYLON_REORDER);
    public static Image MOVE = imageRegistry.get(CEYLON_MOVE);
    public static Image ADD = imageRegistry.get(CEYLON_ADD);
    public static Image ADD_CORR = imageRegistry.get(CEYLON_ADD_CORRECTION);
    public static Image REMOVE_CORR = imageRegistry.get(CEYLON_REMOVE_CORRECTION);
    public static Image LOCAL_NAME = imageRegistry.get(CEYLON_LOCAL_NAME);
    public static Image MULTIPLE_TYPES_IMAGE = imageRegistry.get(MULTIPLE_TYPES);
    public static Image REPO = imageRegistry.get(RUNTIME_OBJ);

    public static Image ERROR = imageRegistry.get(CeylonResources.ERROR);
    public static Image WARNING = imageRegistry.get(CeylonResources.WARNING);

    private static ColorRegistry colorRegistry = PlatformUI.getWorkbench()
            .getThemeManager().getCurrentTheme().getColorRegistry();
    
    public static final Styler ID_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=color(colorRegistry, IDENTIFIERS);
        }
    };
    
    public static final Styler TYPE_ID_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=color(colorRegistry, TYPES);
        }
    };
    
    public static final Styler TYPE_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=color(colorRegistry, TYPES);
        }
    };
    
    public static final Styler KW_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=color(colorRegistry, KEYWORDS);
        }
    };
    
    public static final Styler VERSION_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=color(colorRegistry, STRINGS);
        }
    };
    
    public static final Styler PACKAGE_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=color(colorRegistry, PACKAGES);
        }
    };
    
    private static final Color OLIVE = new Color(Display.getDefault(), 0x80, 0x80, 0);
    
    public static final Styler ARROW_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=OLIVE;
        }
    };
    
    public static final Styler ANN_STYLER = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.foreground=color(colorRegistry, ANNOTATIONS);
        }
    };
    
    private final boolean includePackage;
    
    public CeylonLabelProvider(boolean includePackage) {
        this.includePackage = includePackage;
    }
    
    private static Image getDecoratedImage(Object element, String key) {
        if (key==null) return null;
        int flags = getDecorationAttributes(element);
        ImageDescriptor descriptor = imageRegistry.getDescriptor(key);
        String decoratedKey = key+'#'+flags;
        Image image = imageRegistry.get(decoratedKey);
        if (image==null) {
            imageRegistry.put(decoratedKey, 
                    new DecoratedImageDescriptor(descriptor, flags, new Point(22,16)));
            image = imageRegistry.get(decoratedKey);
        }
        return image;
    }
    
    @Override
    public Image getImage(Object element) {
        return getDecoratedImage(element, getImageKey(element));
    }
    
    private static String getImageKey(Object element) {
        if (element instanceof IFile) {
            return getImageKeyForFile((IFile) element);
        }
        else if (element instanceof IPath) {
            String name = ((IPath) element).lastSegment();
            if (name.equals("module.ceylon")) {
                return CEYLON_MODULE_DESC;
            }
            else if (name.equals("package.ceylon")) {
                return CEYLON_PACKAGE_DESC;
            }
            return CEYLON_FILE;
        }
        if (element instanceof IFolder) {
            IFolder folder = (IFolder) element;
            if (folder.getAdapter(IPackageFragmentRoot.class)!=null) {
                return CEYLON_SOURCE_FOLDER;
            }
            else if (folder.getAdapter(IPackageFragment.class)!=null) {
                return CEYLON_PACKAGE;
            }
            return CEYLON_FOLDER;
        }
        else if (element instanceof IPackageFragmentRoot) { //should be the source folder icon
            return CEYLON_SOURCE_FOLDER;
        }
        if (element instanceof IProject ||
            element instanceof IJavaProject) {
            return CEYLON_PROJECT;
        }
        if (element instanceof CeylonElement) {
            return ((CeylonElement) element).getImageKey();
        }
        if (element instanceof IPackageFragment) {
            if (((IFolder)((IPackageFragment)element).getResource())
                    .getFile("module.ceylon").exists()) {
                return CEYLON_ARCHIVE;
            }
            else {
                return CEYLON_PACKAGE;
            }
        }
        if (element instanceof Package ||
            element instanceof IPackageFragment) {
            return CEYLON_PACKAGE;
        }
        if (element instanceof Module) {
            return CEYLON_ARCHIVE;
        }
        if (element instanceof Unit) {
            return CEYLON_FILE;
        }
        if (element instanceof CeylonOutlineNode) {
            return ((CeylonOutlineNode) element).getImageKey();
        }
        if (element instanceof Node) {
            return getImageKeyForNode((Node) element);
        }
        return CEYLON_FILE;
    }

    public static String getImageKeyForFile(IFile element) {
        String name = element.getName();
        if (name.equals("module.ceylon")) {
            return CEYLON_MODULE_DESC;
        }
        else if (name.equals("package.ceylon")) {
            return CEYLON_PACKAGE_DESC;
        }
        return CEYLON_FILE;
    }
    
    public static String getImageKeyForNode(Node n) {
        if (n instanceof PackageNode) {
            return CEYLON_PACKAGE;
        }
        else if (n instanceof PackageDescriptor) {
            return CEYLON_PACKAGE;
        }
        else if (n instanceof ModuleDescriptor) {
            return CEYLON_ARCHIVE;
        }
        else if (n instanceof Tree.CompilationUnit) {
            return CEYLON_FILE;
        }
        else if (n instanceof Tree.ImportList) {
            return CEYLON_IMPORT_LIST;
        }
        else if (n instanceof Tree.Import || 
        		n instanceof Tree.ImportModule) {
            return CEYLON_IMPORT;
        }
        else if (n instanceof Tree.Declaration) {
            return getImageKeyForDeclarationNode((Tree.Declaration) n);
        }
        else {
            return null;
        }
    }
    
    private static String getImageKeyForDeclarationNode(Tree.Declaration n) {
        boolean shared = hasAnnotation(n.getAnnotationList(), 
                "shared", n.getUnit());
        if (n instanceof Tree.AnyClass) {
            if (shared) {
                return CEYLON_CLASS;
            }
            else {
                return CEYLON_LOCAL_CLASS;
            }
        }
        else if (n instanceof Tree.AnyInterface) {
            if (shared) {
                return CEYLON_INTERFACE;
            }
            else { 
                return CEYLON_LOCAL_INTERFACE;
            }
        }
        else if (n instanceof Tree.AnyMethod) {
            if (shared) {
                return CEYLON_METHOD;
            }
            else {
                return CEYLON_LOCAL_METHOD;
            }
        }
        else if (n instanceof Tree.TypeAliasDeclaration) {
            return CEYLON_ALIAS;
        }
        else {
            if (shared) {
                return CEYLON_ATTRIBUTE;
            }
            else {
                return CEYLON_LOCAL_ATTRIBUTE;
            }
        }
    }
    
    public static Image getImageForDeclaration(Declaration element) {
        return getDecoratedImage(element, getImageKeyForDeclaration(element));
    }

    public static Image getImageForFile(IFile file) {
        return getDecoratedImage(file, getImageKeyForFile(file));
    }

    private static String getImageKeyForDeclaration(Declaration d) {
        if (d==null) return null;
        boolean shared = d.isShared();
        if (d instanceof Class) {
            if (shared) {
                return CEYLON_CLASS;
            }
            else {
                return CEYLON_LOCAL_CLASS;
            }
        }
        else if (d instanceof Interface) {
            if (shared) {
                return CEYLON_INTERFACE;
            }
            else { 
                return CEYLON_LOCAL_INTERFACE;
            }
        }
        else if (d instanceof TypeParameter) {
            return CEYLON_TYPE_PARAMETER;
        }
        else if (d.isParameter()) {
        	if (d instanceof Method) {
        		return CEYLON_PARAMETER_METHOD;
        	}
        	else {
        		return CEYLON_PARAMETER;
        	}
        }
        else if (d instanceof Method) {
            if (shared) {
                return CEYLON_METHOD;
            }
            else {
                return CEYLON_LOCAL_METHOD;
            }
        }
        else if (d instanceof TypeAlias ||
        		d instanceof NothingType) {
            return CEYLON_ALIAS;
        }
        else {
            if (shared) {
                return CEYLON_ATTRIBUTE;
            }
            else {
                return CEYLON_LOCAL_ATTRIBUTE;
            }
        }
    }
    
    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof CeylonOutlineNode) {
            return ((CeylonOutlineNode) element).getLabel();
            //TODO: add the arrow if the node is dirty vs git!
            //return new StyledString("> ", ARROW_STYLER).append(label);
        }
        else if (element instanceof IFile) {
            return new StyledString(((IFile) element).getName());
        }
        else if (element instanceof IPath) {
            return new StyledString(((IPath) element).lastSegment());
        }
        else if (element instanceof IFolder) {
            return new StyledString(((IFolder) element).getName());
        }
        else if (element instanceof IProject) {
            return new StyledString(((IProject) element).getName());
        }
        else if (element instanceof IPackageFragment) {
            return new StyledString(((IPackageFragment) element).getElementName(), 
                    QUALIFIER_STYLER);
        }
        else if (element instanceof IJavaElement) {
            return new StyledString(((IJavaElement) element).getElementName());
        }
        else if (element instanceof CeylonElement) {
            return getStyledLabelForSearchResult((CeylonElement) element);
        }
        else if (element instanceof Package) {
            return new StyledString(getLabel((Package) element), 
                    QUALIFIER_STYLER);
        }
        else if (element instanceof Module) {
        	return new StyledString(getLabel((Module) element));
        }
        else if (element instanceof Unit) {
            return new StyledString(((Unit) element).getFilename());
        }
        else if (element instanceof Node) {
            return getStyledLabelForNode((Node) element);
        }
        else {
            return new StyledString("<something>");
        }
    }

    private StyledString getStyledLabelForSearchResult(CeylonElement ce) {
        String pkg;
        if (includePackage()) {
            pkg = " - " + ce.getPackageLabel();
        }
        else {
            pkg = "";
        }
        IFile file = ce.getFile();
        String path = file==null ? 
        		ce.getVirtualFile().getPath() : 
        		file.getFullPath().toString();
        return new StyledString().append(ce.getLabel())
                .append(pkg, QUALIFIER_STYLER)
                .append(" - " + path, COUNTER_STYLER)
                .append(":" + ce.getLocation(), COUNTER_STYLER);
    }
    
    @Override
    public String getText(Object element) {
        return getStyledText(element).toString();
    }
    
    protected boolean includePackage() {
        return includePackage;
    }
        
    public static StyledString getStyledLabelForNode(Node n) {
        //TODO: it would be much better to render types
        //      from the tree nodes instead of from the
        //      model nodes
        
        if (n instanceof Tree.TypeParameterDeclaration) {
            Tree.TypeParameterDeclaration ac = (Tree.TypeParameterDeclaration) n;
            return new StyledString(name(ac.getIdentifier()));
        }
        if (n instanceof Tree.AnyClass) {
            Tree.AnyClass ac = (Tree.AnyClass) n;
            StyledString label = new StyledString("class ", KW_STYLER);
            label.append(name(ac.getIdentifier()), TYPE_ID_STYLER);
            parameters(ac.getTypeParameterList(), label);
            parameters(ac.getParameterList(), label);
            return label;
        }
        else if (n instanceof Tree.AnyInterface) {
            Tree.AnyInterface ai = (Tree.AnyInterface) n;
            StyledString label = new StyledString("interface ", KW_STYLER);
            label.append(name(ai.getIdentifier()), TYPE_ID_STYLER);
            parameters(ai.getTypeParameterList(), label);
            return label;
        }
        if (n instanceof Tree.TypeAliasDeclaration) {
            Tree.TypeAliasDeclaration ac = (Tree.TypeAliasDeclaration) n;
            StyledString label = new StyledString("alias ", KW_STYLER);
            label.append(name(ac.getIdentifier()), TYPE_ID_STYLER);
            parameters(ac.getTypeParameterList(), label);
            return label;
        }
        else if (n instanceof Tree.ObjectDefinition) {
            Tree.ObjectDefinition ai = (Tree.ObjectDefinition) n;
            return new StyledString("object ", KW_STYLER)
                    .append(name(ai.getIdentifier()), ID_STYLER);
        }
        else if (n instanceof Tree.AttributeSetterDefinition) {
            Tree.AttributeSetterDefinition ai = (Tree.AttributeSetterDefinition) n;
            return new StyledString("assign ", KW_STYLER)
            .append(name(ai.getIdentifier()), ID_STYLER);
        }
        else if (n instanceof Tree.TypedDeclaration) {
            Tree.TypedDeclaration td = (Tree.TypedDeclaration) n;
            Tree.Type tt = td.getType();
            StyledString label = new StyledString();
            label.append(type(tt, td))
                .append(" ")
                .append(name(td.getIdentifier()), ID_STYLER);
            if (n instanceof Tree.AnyMethod) {
                Tree.AnyMethod am = (Tree.AnyMethod) n;
                parameters(am.getTypeParameterList(), label);
                for (Tree.ParameterList pl: am.getParameterLists()) { 
                    parameters(pl, label);
                }
            }
            return label;
        }
        else if (n instanceof Tree.CompilationUnit) {
            Tree.CompilationUnit ai = (Tree.CompilationUnit) n;
            if (ai.getUnit()==null) {
                return new StyledString("unknown");
            }
            return new StyledString(ai.getUnit().getFilename());
        }
        else if (n instanceof Tree.ModuleDescriptor) {
            Tree.ModuleDescriptor i = (Tree.ModuleDescriptor) n;
            Tree.ImportPath p = i.getImportPath();
			if (isNonempty(p)) {
                return new StyledString("module ", KW_STYLER)
                        .append(toPath(p), QUALIFIER_STYLER);
            }
        }
        else if (n instanceof Tree.PackageDescriptor) {
            Tree.PackageDescriptor i = (Tree.PackageDescriptor) n;
            Tree.ImportPath p = i.getImportPath();
			if (isNonempty(p)) {
                return new StyledString("package ", KW_STYLER)
                        .append(toPath(p), QUALIFIER_STYLER);
            }
        }
        else if (n instanceof Tree.ImportList) {
            return new StyledString("imports");
        }
        else if (n instanceof Tree.ImportPath) {
            Tree.ImportPath p = (Tree.ImportPath) n;
            if (isNonempty(p)) {
                return new StyledString(toPath(p), QUALIFIER_STYLER);
            }
        }
        else if (n instanceof Tree.Import) {
            Tree.Import i = (Tree.Import) n;
            Tree.ImportPath p = i.getImportPath();
			if (isNonempty(p)) {
                return new StyledString(toPath(p), QUALIFIER_STYLER);
            }
        }
        else if (n instanceof Tree.ImportModule) {
            Tree.ImportModule i = (Tree.ImportModule) n;
            Tree.ImportPath p = i.getImportPath();
			if (isNonempty(p)) {
                return new StyledString(toPath(p), QUALIFIER_STYLER);
            }
			Tree.QuotedLiteral ql = i.getQuotedLiteral();
			if (ql!=null) {
				return new StyledString(ql.getText().replace("'", ""), 
						QUALIFIER_STYLER);
			}
        }
        else if (n instanceof PackageNode) {
            PackageNode pn = (PackageNode) n;
            if (pn.getPackageName().isEmpty()) {
                return new StyledString("default package");
            }
            else {
                return new StyledString(pn.getPackageName(), QUALIFIER_STYLER);
            }
        }
        
        return new StyledString("<something>");
    }

	private static boolean isNonempty(Tree.ImportPath p) {
		return p!=null && !p.getIdentifiers().isEmpty();
	}

    private static String toPath(Tree.ImportPath p) {
        return formatPath(p.getIdentifiers());
    }
    
    private static StyledString type(Tree.Type type, Tree.TypedDeclaration node) {
        StyledString result = new StyledString();
        if (type!=null) {
            if (type instanceof Tree.VoidModifier) {
                return result.append("void", KW_STYLER);
            }
            if (type instanceof Tree.DynamicModifier) {
                return result.append("dynamic", KW_STYLER);
            }
            ProducedType tm = type.getTypeModel();
        	if (tm!=null && !isTypeUnknown(tm)) {
        		boolean sequenced = type instanceof Tree.SequencedType;
        		if (sequenced) {
        			tm = type.getUnit().getIteratedType(tm);
                	if (tm!=null) {
                		return result.append(tm.getProducedTypeName(node.getUnit()), 
                		            TYPE_STYLER)
                		        .append("*");
                	}
        		}
				return result.append(tm.getProducedTypeName(node.getUnit()), 
                        TYPE_STYLER);
        	}
        }
        return result.append(node instanceof Tree.AnyMethod ? "function" : "value", 
                KW_STYLER);
    }
    
    private static String name(Tree.Identifier id) {
        if (id==null || id.getText().startsWith("<missing")) {
            return "<unknown>";
        }
        else {
            return id.getText();
        }
    }
    
    private static void parameters(Tree.ParameterList pl, StyledString label) {
        if (pl==null ||
                pl.getParameters().isEmpty()) {
            label.append("()");
        }
        else {
            label.append("(");
            int len = pl.getParameters().size(), i=0;
            for (Tree.Parameter p: pl.getParameters()) {
                if (p!=null) {
                    if (p instanceof Tree.ParameterDeclaration) {
                        Tree.TypedDeclaration td = ((Tree.ParameterDeclaration) p).getTypedDeclaration();
                        label.append(type(td.getType(), td))
                            .append(" ")
                            .append(name(td.getIdentifier()), ID_STYLER);
                        if (p instanceof Tree.FunctionalParameterDeclaration) {
                            for (Tree.ParameterList ipl: ((Tree.MethodDeclaration) td).getParameterLists()) {
                                parameters(ipl, label);
                            }
                        }
                    }
                    else if (p instanceof Tree.InitializerParameter) {
                        label.append(name(((Tree.InitializerParameter) p).getIdentifier()), ID_STYLER);
                    }
                }
                if (++i<len) label.append(", ");
            }
            label.append(")");
        }
    }
    
    private static void parameters(Tree.TypeParameterList tpl, StyledString label) {
        if (tpl!=null &&
                !tpl.getTypeParameterDeclarations().isEmpty()) {
            label.append("<");
            int len = tpl.getTypeParameterDeclarations().size(), i=0;
            for (Tree.TypeParameterDeclaration p: tpl.getTypeParameterDeclarations()) {
                label.append(name(p.getIdentifier()), TYPE_STYLER);
                if (++i<len) label.append(", ");
            }
            label.append(">");
        }
    }
    
    @Override
    public void addListener(ILabelProviderListener listener) {
        fListeners.add(listener);
    }
    
    @Override
    public void dispose() {}
    
    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }
    
    @Override
    public void removeListener(ILabelProviderListener listener) {
        fListeners.remove(listener);
    }
    
    public static String getLabel(Package packageModel) {
        String name = packageModel.getQualifiedNameString();
        if (name.isEmpty()) name="default package";
        return name;
    }
    
    public static String getLabel(Module moduleModel) {
        String name = moduleModel.getNameAsString();
        if (name.isEmpty()) name="default module";
        return name;
    }
    
    public static String getPackageLabel(Node decl) {
        return decl.getUnit()==null ? "unknown package" : 
            getLabel(decl.getUnit().getPackage());
    }
    
    public static String getModuleLabel(Node decl) {
        return decl.getUnit()==null ? "unknown module" : 
            getLabel(decl.getUnit().getPackage().getModule());
    }
    
    public static String getModuleLabel(Declaration decl) {
        return decl.getUnit()==null ? "unknown module" : 
            getLabel(decl.getUnit().getPackage().getModule());
    }
    
    public static String getPackageLabel(Declaration decl) {
        return getLabel(decl.getUnit().getPackage());
    }
    
    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        StyledString styledText = getStyledText(element);
        cell.setText(styledText.toString());
        cell.setStyleRanges(styledText.getStyleRanges());
        cell.setImage(getImage(element));
        super.update(cell);
    }
    
}
