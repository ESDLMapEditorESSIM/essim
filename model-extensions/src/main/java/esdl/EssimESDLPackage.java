/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */
package esdl;

/** This package interface extends  the generated package interface 
    It is necessary because its name is used in the EMF generated code) 
 */
public interface EssimESDLPackage extends EsdlPackage 
{
	EssimESDLPackage eINSTANCE = esdl.impl.EssimESDLPackageImpl.init();

}
