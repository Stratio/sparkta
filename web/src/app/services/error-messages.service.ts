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
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Injectable()
export class ErrorMessagesService {
    public errors: any;

    constructor(private translate: TranslateService) {
        const errorMessages = ['ERRORS.INPUTS.GENERIC', 'ERRORS.INPUTS.REQUIRED', 'ERRORS.INPUTS.MINLENGTH', 'ERRORS.INPUTS.MAXLENGTH',
            'ERRORS.INPUTS.MIN', 'ERRORS.INPUTS.MAX', 'ERRORS.INPUTS.PATTERN'];

        this.translate.get(errorMessages).subscribe(
            (value: { [key: string]: string }) => {
                this.errors = {
                    inputErrors: {
                        generic: value['ERRORS.INPUTS.GENERIC'],
                        required: value['ERRORS.INPUTS.REQUIRED'],
                        minLength: value['ERRORS.INPUTS.MINLENGTH'],
                        maxLength: value['ERRORS.INPUTS.MAXLENGTH'],
                        min: value['ERRORS.INPUTS.MIN'],
                        max: value['ERRORS.INPUTS.MAX'],
                        pattern: value['ERRORS.INPUTS.PATTERN']
                    },
                    selectRequiredError: value['ERRORS.INPUTS.REQUIRED']
                };
            }
        );
    }
}