import LaunchRoundedIcon from '@mui/icons-material/LaunchRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import { Box, Chip, Divider, Link, List, ListItem, Stack, Typography } from '@mui/material';
import type { AlertItem } from '../models/alert';
import {
  countyChipSx,
  formatSeverity,
  normalizeResourceKey,
  resourceChipSx,
  resourceLabels,
  severityChipSx
} from '../lib/alertMeta';

interface AlertListProps {
  alerts: AlertItem[];
}

function formatTimestamp(value?: string) {
  if (!value) {
    return 'No timestamp';
  }

  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function AlertList({ alerts }: AlertListProps) {
  if (alerts.length === 0) {
    return (
      <Box className="empty-state">
        <WarningAmberRoundedIcon color="disabled" />
        <Typography variant="h6">No alerts match the current filters.</Typography>
        <Typography color="text.secondary">
          Try a different county selection or lower the severity threshold.
        </Typography>
      </Box>
    );
  }

  return (
    <List className="alert-list">
      {alerts.map((alert, index) => (
        <Box key={`${alert.source}-${alert.id}`}>
          <ListItem className="alert-list__item" disableGutters alignItems="flex-start">
            <Stack spacing={2} width="100%">
              <Stack
                direction={{ xs: 'column', md: 'row' }}
                justifyContent="space-between"
                spacing={2}
              >
                <Stack spacing={1}>
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                    <Chip
                      label={resourceLabels[normalizeResourceKey(alert.source)] || alert.source}
                      size="small"
                      sx={
                        resourceChipSx[normalizeResourceKey(alert.source)] || resourceChipSx.polisen
                      }
                    />
                    <Chip
                      label={formatSeverity(alert.severity)}
                      size="small"
                      sx={
                        severityChipSx[alert.severity as keyof typeof severityChipSx] ||
                        severityChipSx.info
                      }
                    />
                    {alert.areas?.map((area) => (
                      <Chip
                        key={area}
                        label={area}
                        size="small"
                        variant="outlined"
                        sx={countyChipSx}
                      />
                    ))}
                  </Stack>
                  <Typography variant="h6">{alert.headline}</Typography>
                  <Typography color="text.secondary" variant="body2">
                    {formatTimestamp(alert.publishedAt)}
                  </Typography>
                </Stack>
                {alert.url && (
                  <Link
                    className="alert-list__link"
                    href={alert.url}
                    target="_blank"
                    rel="noreferrer"
                    underline="none"
                  >
                    Open source <LaunchRoundedIcon fontSize="inherit" />
                  </Link>
                )}
              </Stack>
              <Typography variant="body1">{alert.description}</Typography>
            </Stack>
          </ListItem>
          {index < alerts.length - 1 && <Divider component="li" />}
        </Box>
      ))}
    </List>
  );
}

export default AlertList;
