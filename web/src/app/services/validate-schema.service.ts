///
/// Copyright (C) 2015 Stratio (http://stratio.com)
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///         http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { ValidationModel, ValidationErrorModel } from 'app/models/validation-schema.model';

@Injectable()
export class ValidateSchemaService {
    public errors: any;


    public static setDefaultWorkflowSettings(value: any): any {
        let model: any = {};
        model.basic = {
            name: 'Workflow-name',
            description: 'workflow description'
        };
        model.advancedSettings = {};
        value.advancedSettings.map((category: any) => {
            model.advancedSettings[category.name] = this.getCategoryModel(category.properties);
        });
        return model;
    }


    public static getCategoryModel(value: any): any {
        let model: any = {};
        value.map((prop: any) => {
            if (prop.properties) {
                model[prop.name] = this.getCategoryModel(prop.properties);
            } else {
                model[prop.propertyId] = prop.default ? prop.default : null;
            }
        });
        return model;
    }

    validateModel(): ValidationModel {
        return null;
    }



    getEntityRules() {


    }





    getErrors(): Array<ValidationErrorModel> {
        return [];
    }

    private _validRequired(value: any): ValidationErrorModel {
        if (value && value.toString().length > 0) {
            return null;
        }
    }

    setDefaultEntityModel(value: any): any {
        let model: any = {};
        model.configuration = {};
        value.properties.map((prop: any) => {
            model.configuration[prop.propertyId] = prop.default ? prop.default : null;
        });
        model.classPrettyName = value.classPrettyName;
        model.className = value.className;
        model.stepType = value.stepType;
        model.description = value.description;

        return model;
    }


    constructor() {

    }
}


