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

import { Component, OnInit, Output, EventEmitter, Input, ViewChildren } from '@angular/core';

@Component({
    selector: 'form-file',
    templateUrl: './form-file.template.html',
    styleUrls: ['./form-file.styles.scss']
})
export class FormFileComponent implements OnInit {

    @Input() text: any;
    @Input() icon = 'icon-upload';
    @Input() type = 'button-primary';
    @Output() onFileUpload = new EventEmitter<string>();

    @ViewChildren('fileUpload') vc: any;

    ngOnInit(): void { }

    onChange(event: any) {
        this.onFileUpload.emit(event.target.files);
        this.vc.first.nativeElement.value = '';
    }

    constructor() { }

}

