// SPDX-FileCopyrightText: 2019-2021 Matus Faro <matus@smotana.com>
// SPDX-License-Identifier: AGPL-3.0-only
import { Button, Collapse } from '@material-ui/core';
import { createStyles, Theme, withStyles, WithStyles } from '@material-ui/core/styles';
import classNames from 'classnames';
import React, { Component } from 'react';
import { Server } from '../../api/server';
import RichEditor from '../../common/RichEditor';
import RichEditorImageUpload from '../../common/RichEditorImageUpload';
import ScrollAnchor from '../../common/util/ScrollAnchor';

const styles = (theme: Theme) => createStyles({
  addCommentForm: {
    display: 'inline-flex',
    flexDirection: 'column',
    margin: theme.spacing(1, 0),
    alignItems: 'flex-end',
    /**
     * Width is complicated here:
     * - Should not be less than 300 unless parent is shorter: minWidth: min(100%, 300px)
     * - Should be default 300 and expand with text content: width: max-content
     * - Should be max 600 unless parent is shorter: maxWidth 100% AND 600px spread out in addCommentFormOuter
     */
    minWidth: 'min(100%, 300px)',
    width: 'max-content',
    maxWidth: '100%',
  },
  addCommentFormOuter: {
    maxWidth: 600,
  },
  addCommentField: {
    transition: theme.transitions.create('width'),
    width: '100%',
  },
  addCommentSubmitButton: {
    margin: theme.spacing(1),
  },
});

interface Props {
  className?: string;
  collapseIn?: boolean;
  focusOnIn?: boolean;
  server: Server;
  ideaId: string;
  parentCommentId?: string;
  inputLabel?: string;
  logIn: () => Promise<void>;
  onSubmitted?: () => void;
  onBlurAndEmpty?: () => void;
}

interface State {
  newCommentInput?: string;
}

class Post extends Component<Props & WithStyles<typeof styles, true>, State> {
  state: State = {};
  readonly richEditorImageUploadRef = React.createRef<RichEditorImageUpload>();
  readonly inputRef: React.RefObject<HTMLInputElement> = React.createRef();

  componentDidMount() {
    if (this.props.focusOnIn && this.props.collapseIn === true) {
      this.inputFocus();
    };
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (this.props.focusOnIn
      && prevProps.collapseIn !== true
      && this.props.collapseIn === true) {
      this.inputFocus();
    };
  }

  inputFocus() {
    // Focus after smooth scrolling finishes to prevent rapid scroll
    setTimeout(() => this.inputRef.current?.focus(), 200);
  }

  render() {
    return (
      <Collapse
        in={this.props.collapseIn !== false}
        className={classNames(this.props.className, this.props.classes.addCommentFormOuter)}
      >
        <div className={this.props.classes.addCommentForm}>
          <RichEditor
            uploadImage={(file) => this.richEditorImageUploadRef.current!.uploadImage(file)}
            variant='outlined'
            size='small'
            id='createComment'
            className={this.props.classes.addCommentField}
            label={this.props.inputLabel}
            iAgreeInputIsSanitized
            minInputHeight={60}
            value={this.state.newCommentInput || ''}
            onChange={e => this.setState({ newCommentInput: e.target.value })}
            multiline
            rowsMax={10}
            InputProps={{
              inputRef: this.inputRef,
              // onBlurAndEmpty after a while, fixes issue where pasting causes blur.
              onBlur: () => setTimeout(() => !this.state.newCommentInput && this.props.onBlurAndEmpty && this.props.onBlurAndEmpty(), 200),
            }}
          />
          <RichEditorImageUpload
            ref={this.richEditorImageUploadRef}
            server={this.props.server}
          />
          <Collapse in={!!this.state.newCommentInput}>
            <Button
              color='primary'
              variant='contained'
              disableElevation
              className={this.props.classes.addCommentSubmitButton}
              disabled={!this.state.newCommentInput}
              onClick={e => {
                this.props.logIn().then(() => this.props.server.dispatch().then(d => d.commentCreate({
                  projectId: this.props.server.getProjectId(),
                  ideaId: this.props.ideaId,
                  commentCreate: {
                    content: this.state.newCommentInput!,
                    parentCommentId: this.props.parentCommentId,
                  },
                }))).then(comment => {
                  this.setState({ newCommentInput: undefined })
                  this.props.onSubmitted && this.props.onSubmitted();
                });
              }}
            >
              Post
            </Button>
          </Collapse>
          {!!this.props.focusOnIn && (
            <ScrollAnchor scrollNow={this.props.collapseIn} />
          )}
        </div>
      </Collapse>
    );
  }
}

export default withStyles(styles, { withTheme: true })(Post);
