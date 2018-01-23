import { Subscription } from 'rxjs/Rx';
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

import { ChangeDetectionStrategy, Component, EventEmitter, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Store } from '@ngrx/store';
import * as workflowActions from './../../actions/workflow-list';
import * as fromRoot from './../../reducers';

@Component({
    selector: 'workflow-group-modal',
    templateUrl: './workflow-group-modal.template.html',
    styleUrls: ['./workflow-group-modal.styles.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class WorkflowGroupModal implements OnInit, OnDestroy {

    @Output() onCloseGroupModal = new EventEmitter<string>();
    @ViewChild('groupForm') public groupForm: NgForm;

    public forceValidations = false;
    public name = '';

    private openModal$: Subscription;

    constructor(private _store: Store<fromRoot.State>) {
        _store.dispatch(new workflowActions.InitCreateGroupAction());
    }

    createGroup() {
        if (this.groupForm.valid) {
            this._store.dispatch(new workflowActions.CreateGroupAction(this.name));
        } else {
            this.forceValidations = true;
        }
    }

    ngOnInit() {
        this.openModal$ = this._store.select(fromRoot.getCreateGroupModalOpen).subscribe((modalOpen) => {
            if (!modalOpen) {
                this.onCloseGroupModal.emit();
            }
        });
    }

    ngOnDestroy(): void {
        this.openModal$ && this.openModal$.unsubscribe();
    }

}
