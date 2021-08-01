// SPDX-FileCopyrightText: 2019-2020 Matus Faro <matus@smotana.com>
// SPDX-License-Identifier: AGPL-3.0-only
import { IconButton } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import DismissIcon from '@material-ui/icons/CloseRounded';
import { ProviderContext, SnackbarProvider } from 'notistack';
import React from 'react';

const muiSnackbarStyles = makeStyles({
  dismissButton: {
    color: 'white',
  },
});
const MuiSnackbarProvider = (props: {
  children: React.ReactNode;
  notistackRef?: React.RefObject<ProviderContext>;
}) => {
  const notistackRef = props.notistackRef || React.createRef<ProviderContext>();
  const classes = muiSnackbarStyles();
  return (
    <SnackbarProvider
      ref={notistackRef}
      preventDuplicate
      maxSnack={3}
      hideIconVariant
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
      action={key => (
        <IconButton
          className={classes.dismissButton}
          onClick={() => notistackRef.current?.closeSnackbar(key)}
        >
          <DismissIcon fontSize='inherit' />
        </IconButton>
      )}
    >
      {props.children}
    </SnackbarProvider>
  )
};

export default MuiSnackbarProvider;
